package com.auguigu.gulimall.search.controller;

import com.auguigu.gulimall.search.service.MallSearchService;
import com.auguigu.gulimall.search.vo.SearchParam;
import com.auguigu.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {


    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        //通过request拿到原生的查询条件
        String queryString = request.getQueryString();
        param.set_queryString(queryString);

        //1.根据传递来的页面的查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result",result);
        return "list";
    }






}
