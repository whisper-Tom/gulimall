package com.auguigu.gulimall.search.service;

import com.auguigu.gulimall.search.vo.SearchParam;
import com.auguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
