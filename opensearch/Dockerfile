FROM opensearchproject/opensearch:2.11.0

# 安装 IK Analysis 分词插件（中文分词）
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch https://get.infini.cloud/opensearch/analysis-ik/2.11.0

# 安装 Pinyin 拼音插件
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch https://get.infini.cloud/opensearch/analysis-pinyin/2.11.0
