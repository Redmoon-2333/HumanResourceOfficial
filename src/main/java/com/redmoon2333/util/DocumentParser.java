package com.redmoon2333.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档解析工具类
 * 支持TXT、DOCX、PDF格式的文档解析
 */
public class DocumentParser {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentParser.class);
    
    /**
     * 根据文件类型解析文档内容
     * 
     * @param filePath 文件路径
     * @return 解析后的文本内容
     * @throws IOException 文件读取异常
     */
    public static String parseDocument(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        logger.debug("开始解析文档: {}", fileName);
        
        if (fileName.endsWith(".txt")) {
            return parseTxtFile(filePath);
        } else if (fileName.endsWith(".docx")) {
            return parseDocxFile(filePath);
        } else if (fileName.endsWith(".pdf")) {
            return parsePdfFile(filePath);
        } else {
            throw new IllegalArgumentException("不支持的文件格式: " + fileName);
        }
    }
    
    /**
     * 解析TXT文件
     * 
     * @param filePath 文件路径
     * @return 文本内容
     * @throws IOException 文件读取异常
     */
    private static String parseTxtFile(Path filePath) throws IOException {
        logger.debug("解析TXT文件: {}", filePath.getFileName());
        
        // 读取UTF-8编码的文本文件
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        
        logger.debug("TXT文件解析完成，内容长度: {} 字符", content.length());
        return content;
    }
    
    /**
     * 解析DOCX文件
     * 
     * @param filePath 文件路径
     * @return 文本内容
     * @throws IOException 文件读取异常
     */
    private static String parseDocxFile(Path filePath) throws IOException {
        logger.debug("解析DOCX文件: {}", filePath.getFileName());
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String content = extractor.getText();
            logger.debug("DOCX文件解析完成，内容长度: {} 字符", content.length());
            return content;
            
        } catch (Exception e) {
            logger.error("DOCX文件解析失败: {}", filePath.getFileName(), e);
            throw new IOException("DOCX文件解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析PDF文件
     * 
     * @param filePath 文件路径
     * @return 文本内容
     * @throws IOException 文件读取异常
     */
    private static String parsePdfFile(Path filePath) throws IOException {
        logger.debug("解析PDF文件: {}", filePath.getFileName());
        
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // 设置提取参数
            stripper.setSortByPosition(true);
            
            String content = stripper.getText(document);
            logger.debug("PDF文件解析完成，内容长度: {} 字符", content.length());
            return content;
            
        } catch (Exception e) {
            logger.error("PDF文件解析失败: {}", filePath.getFileName(), e);
            throw new IOException("PDF文件解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查文件是否为支持的格式
     * 
     * @param fileName 文件名
     * @return 是否支持
     */
    public static boolean isSupportedFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".txt") || 
               lowerName.endsWith(".docx") || 
               lowerName.endsWith(".pdf");
    }
}
