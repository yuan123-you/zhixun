package com.zhixun.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.Introspector;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommentTitleContractTest {
    @Test
    void articleTitleRemainsAReadableWritableStringJsonProperty() throws Exception {
        var property = Arrays.stream(Introspector.getBeanInfo(CommentVO.class).getPropertyDescriptors())
                .filter(p -> p.getName().equals("articleTitle")).findFirst().orElseThrow();
        assertEquals(String.class, property.getPropertyType());
        assertNotNull(property.getReadMethod());
        assertNotNull(property.getWriteMethod());
        CommentVO vo = new CommentVO();
        property.getWriteMethod().invoke(vo, "保留的作品标题");
        assertEquals("保留的作品标题", property.getReadMethod().invoke(vo));
        var json = new ObjectMapper().valueToTree(vo);
        assertEquals("保留的作品标题", json.path("articleTitle").asText());
    }
}
