#!/bin/bash

# RAG功能快速部署脚本

echo "=========================================="
echo "  RAG功能快速部署脚本"
echo "=========================================="
echo ""

# 检查Docker是否安装
echo "1. 检查Docker环境..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装，请先安装Docker"
    exit 1
fi
echo "✅ Docker已安装"

# 检查docker-compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose未安装，请先安装docker-compose"
    exit 1
fi
echo "✅ docker-compose已安装"
echo ""

# 启动Qdrant
echo "2. 启动Qdrant服务..."
docker-compose -f docker-compose-qdrant.yml up -d

# 等待Qdrant启动
echo "等待Qdrant服务启动（10秒）..."
sleep 10

# 检查Qdrant是否启动成功
echo ""
echo "3. 检查Qdrant服务状态..."
if curl -s http://localhost:6333/ > /dev/null 2>&1; then
    echo "✅ Qdrant服务已成功启动"
    echo "   - HTTP API: http://localhost:6333"
    echo "   - gRPC API: localhost:6334"
else
    echo "❌ Qdrant服务启动失败"
    echo "请运行以下命令查看日志："
    echo "  docker-compose -f docker-compose-qdrant.yml logs"
    exit 1
fi

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "下一步操作："
echo "1. 配置application.yml中的RAG相关配置"
echo "2. 准备知识库文件到: src/main/resources/rag-knowledge-base/"
echo "3. 启动后端服务: mvn spring-boot:run"
echo "4. 访问管理页面: http://localhost:5173/rag-management"
echo "5. 点击'初始化知识库'按钮"
echo ""
echo "查看Qdrant管理界面: http://localhost:6333/dashboard"
echo ""
