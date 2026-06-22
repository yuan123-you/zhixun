package com.zhixun.vo;

import java.util.List;
import lombok.Data;

@Data
public class SearchSuggestResultVO {
    private List<SuggestionVO> completions;
    private List<String> hotSearches;
}
