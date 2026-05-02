package com.wpb.spky.api;

import com.wpb.spky.persistence.QueryTemplateRepository;
import com.wpb.spky.persistence.QueryTemplateRepository.QueryTemplateRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/query-templates")
@ConditionalOnBean(QueryTemplateRepository.class)
@RequiredArgsConstructor
public class QueryTemplateController {

    private final QueryTemplateRepository queryTemplates;

    @GetMapping
    public List<QueryTemplateRecord> list() {
        return queryTemplates.findEnabled();
    }
}
