package com.auguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.auguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.auguigu.gulimall.search.constant.EsConstant;
import com.auguigu.gulimall.search.feign.ProductFeignService;
import com.auguigu.gulimall.search.service.MallSearchService;
import com.auguigu.gulimall.search.vo.AttrResponseVo;
import com.auguigu.gulimall.search.vo.SearchParam;
import com.auguigu.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    //去es进行检索
    @Override
    public SearchResult search(SearchParam param) {

        //动态构建出查询需要的DSL语句
        SearchResult result = null;
        //准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            //3.分析
            result = buildSearchResult(response,param);
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 准备检索请求
     * 模糊匹配，过滤（按照属性，分类，品牌，加个区间，库存），排序，分页，高亮，聚合分析
     * 模糊匹配，过滤（按照属性，分类，品牌，加个区间，库存），
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//构建DSL语句

        /**
         * 查询条件：模糊匹配，过滤（按照属性，分类，品牌，加个区间，库存）
         */

        //1.构建bool - query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //1.1、must  模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        //1.2 bool - filter  按照三级分类来查
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }



        //1.2 bool - filter  按照品牌id去查
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            for(Long brandId : param.getBrandId()){
                boolQuery.filter(QueryBuilders.termQuery("brandId",brandId));
            }
            //boolQuery.filter(QueryBuilders.termQuery("brandId", param.getBrandId()));
        }

        //1.2 bool - filter - 按照所有指定的属性进行查询
        if (param.getAttrs()!=null && param.getAttrs().size()>0){

            for(String attrStr : param.getAttrs()){
                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];//检索的属性id
                String[] attrValues = s[1].split(":");//这个属性的检索用的值
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrValue",attrValues));

                //每一个必须都得生成一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);

            }

        }
        //1.2 bool - filter - 按照库存是否有进行查询
        if (param.getHasStock()!=null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        //1.2 bool - filter - 按照价格区间进行查询
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            //1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(s[0]);
                }
                if(param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }
        }
        //把以上所有的条件都拿来进行封装
        sourceBuilder.query(boolQuery);
        /**
         *排序，分页，高亮，聚合分析
         */
        //2.1 排序
        if(!StringUtils.isEmpty(param.getSort())){
            //sort=hotScore_asc/desc
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0],order);

        }

        //2.2 分页
        sourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3 高亮
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);

        }

        //聚合分析
        //5.1 按照brand聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brandId");
        TermsAggregationBuilder brandNameAgg = AggregationBuilders.terms("brandNameAgg").field("brandName");
        TermsAggregationBuilder brandImgAgg = AggregationBuilders.terms("brandImgAgg").field("brandImg");
        brandAgg.subAggregation(brandNameAgg);
        brandAgg.subAggregation(brandImgAgg);
        sourceBuilder.aggregation(brandAgg);

        //5.2 按照catalog聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalogAgg").field("catalogId");
        TermsAggregationBuilder catalogNameAgg = AggregationBuilders.terms("catalogNameAgg").field("catalogName");
        catalogAgg.subAggregation(catalogNameAgg);
        sourceBuilder.aggregation(catalogAgg);

        //5.3 按照attrs聚合
        NestedAggregationBuilder nestedAggregationBuilder = new NestedAggregationBuilder("attrs", "attr");
        //按照attrId聚合
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attrIdAgg").field("attr.attrId");
        //按照attrId聚合之后再按照attrName和attrValue聚合
        TermsAggregationBuilder attrNameAgg = AggregationBuilders.terms("attrNameAgg").field("attr.attrName");
        TermsAggregationBuilder attrValueAgg = AggregationBuilders.terms("attrValueAgg").field("attr.attrValue");
        attrIdAgg.subAggregation(attrNameAgg);
        attrIdAgg.subAggregation(attrValueAgg);

        nestedAggregationBuilder.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(nestedAggregationBuilder);


        String s = sourceBuilder.toString();
        System.out.println("构建的DSL"+s);


        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 构建结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        SearchHits hits = response.getHits();
        //1. 封装查询到的商品信息
        if (hits.getHits()!=null&&hits.getHits().length>0){
            List<SkuEsModel> skuEsModels = new ArrayList<>();
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                //设置高亮属性
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLight = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(highLight);
                }
                skuEsModels.add(skuEsModel);
            }
            result.setProduct(skuEsModels);
        }

        //2. 封装分页信息
        //2.1 当前页码
        result.setPageNum(param.getPageNum());
        //2.2 总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        //2.3 总页码
        Integer totalPages = (int)total % EsConstant.PRODUCT_PAGESIZE == 0 ?
                (int)total / EsConstant.PRODUCT_PAGESIZE : (int)total / EsConstant.PRODUCT_PAGESIZE + 1;
        result.setTotalPages(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //3. 查询结果涉及到的品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        Aggregations aggregations = response.getAggregations();
        //ParsedLongTerms用于接收terms聚合的结果，并且可以把key转化为Long类型的数据
        ParsedLongTerms brandAgg = aggregations.get("brandAgg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            //3.1 得到品牌id
            Long brandId = bucket.getKeyAsNumber().longValue();

            Aggregations subBrandAggs = bucket.getAggregations();
            //3.2 得到品牌图片
            ParsedStringTerms brandImgAgg=subBrandAggs.get("brandImgAgg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            //3.3 得到品牌名字
            Terms brandNameAgg=subBrandAggs.get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo(brandId, brandName, brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //4. 查询涉及到的所有分类
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = aggregations.get("catalogAgg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            //4.1 获取分类id
            Long catalogId = bucket.getKeyAsNumber().longValue();
            Aggregations subcatalogAggs = bucket.getAggregations();
            //4.2 获取分类名
            ParsedStringTerms catalogNameAgg=subcatalogAggs.get("catalogNameAgg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo(catalogId, catalogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //5 查询涉及到的所有属性
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        //ParsedNested用于接收内置属性的聚合
        ParsedNested parsedNested=aggregations.get("attrs");
        ParsedLongTerms attrIdAgg=parsedNested.getAggregations().get("attrIdAgg");
        if(attrIdAgg!=null){
            for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
                //5.1 查询属性id
                Long attrId = bucket.getKeyAsNumber().longValue();

                Aggregations subAttrAgg = bucket.getAggregations();
                //5.2 查询属性名
                ParsedStringTerms attrNameAgg=subAttrAgg.get("attrNameAgg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                //5.3 查询属性值
                ParsedStringTerms attrValueAgg = subAttrAgg.get("attrValueAgg");
                List<String> attrValues = new ArrayList<>();
                for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
                    String attrValue = attrValueAggBucket.getKeyAsString();
                    attrValues.add(attrValue);
                    List<SearchResult.NavVo> navVos = new ArrayList<>();
                }
                SearchResult.AttrVo attrVo = new SearchResult.AttrVo(attrId, attrName, attrValues);
                attrVos.add(attrVo);
            }
        }

        result.setAttrs(attrVos);

         //6. 构建面包屑导航，不过因为es上架的那里有问题还没解决，上架了但是属性添加不到es里面去，导致现在没有面包屑
        //但是这里是有疑问的，第一个是不明白为什么要在后台做这个路径的操作，为什么不在前端做
        //还有就是属性名的获取，这里是通过for循环去掉远程服务，这应该很耗资源的
        List<String> attrs = param.getAttrs();
        if (attrs != null && attrs.size() > 0) {
            List<SearchResult.NavVo> navVos = attrs.stream().map(attr -> {
                String[] split = attr.split("_");
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //6.1 设置属性值
                navVo.setNavValue(split[1]);
                //6.2 查询并设置属性名
                try {
                    R r = productFeignService.getAttrsInfo(Long.parseLong(split[0]));
                    if (r.getCode() == 0) {
                        AttrResponseVo attrResponseVo = JSON.parseObject(JSON.toJSONString(r.get("attr")), new TypeReference<AttrResponseVo>() {
                        });
                        navVo.setNavName(attrResponseVo.getAttrName());
                    }
                } catch (Exception e) {
                    log.error("远程调用商品服务查询属性失败", e);
                }
                //6.3 设置面包屑跳转链接
                String encode = null;
                try {
                    encode = URLEncoder.encode(attr,"UTF-8");
                    encode.replace("+","%20");//URLEncoder在转义的时候会把空格转为+号，因此必须replace
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String queryString = param.get_queryString();
                String replace = queryString.replace("&attrs=" + encode, "").replace("attrs=" + attr+"&", "").replace("attrs=" + attr, "");
                navVo.setLink("http://localhost:16000/list.html" + (replace.isEmpty()?"":"?"+replace));
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
        }
        return result;
    }

}
