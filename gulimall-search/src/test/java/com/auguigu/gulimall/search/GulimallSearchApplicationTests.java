package com.auguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.auguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {


    @Autowired
    private RestHighLevelClient client;


    @Test
    void contextLoads() {

        System.out.println(client);


    }


    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setUsername("张三2");
        user.setAge(18);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //IndexResponse[index=users,type=_doc,id=1,version=1,result=created,seqNo=0,primaryTerm=1,shards={"total":2,"successful":1,"failed":0}]
        //IndexResponse[index=users,type=_doc,id=1,version=2,result=updated,seqNo=1,primaryTerm=1,shards={"total":2,"successful":1,"failed":0}]
        System.out.println(index);

    }


    /*
    聚合函数
     */
    @Test
    public void searchData() throws IOException{
        SearchSourceBuilder builder = new SearchSourceBuilder();
        //获取家庭住址
        builder.query(QueryBuilders.matchQuery("address", "mill"));

        //根据年龄划分
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age");
        builder.aggregation(ageAgg);

        //获取平均年龄
        AvgAggregationBuilder ageAvg = AggregationBuilders.avg("ageAvg").field("age");
        builder.aggregation(ageAvg);

        //获取平均工资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        builder.aggregation(balanceAvg);

        SearchRequest searchRequest = new SearchRequest();
//        searchRequest.indices("bank");
        searchRequest.source(builder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        System.out.println("request:"+searchRequest);
//        System.out.println("response:" + searchResponse);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit documentFields : hits1) {
            String sourceAsString = documentFields.getSourceAsString();
            System.out.println(sourceAsString);
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account);
        }

        Aggregations aggregations = searchResponse.getAggregations();
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println(keyAsString+"---"+bucket.getDocCount());
        }

        Avg ageAvg1 = aggregations.get("ageAvg");
        System.out.println("ageAvg:"+ageAvg1.getValue());

        Avg balanceAvg1  = aggregations.get("balanceAvg");
        System.out.println("balanceAvg:"+balanceAvg1.getValue());


    }


    @Setter
    @Getter
    class User {
        private int age;
        private String username;
        private String gender;
    }

    @ToString
    @Data
    static class Account {
        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }
}
