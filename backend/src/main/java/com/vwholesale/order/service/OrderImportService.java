package com.vwholesale.order.service;

import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class OrderImportService {

    private static final Set<String> ALLOWED_EXT = Set.of("xlsx", "xls");
    private static final DataFormatter FORMATTER = new DataFormatter();

    public String parseExcel(MultipartFile file) {
        RoleChecker.requireCustomer();
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(400, "请选择 Excel 文件");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw BusinessException.of(400, "仅支持 .xlsx / .xls 文件");
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw BusinessException.of(400, "Excel 中没有可读取的工作表");
            }
            List<String> lines = new ArrayList<>();
            for (Row row : sheet) {
                String line = rowToText(row);
                if (StringUtils.hasText(line)) {
                    lines.add(line.trim());
                }
            }
            if (lines.isEmpty()) {
                throw BusinessException.of(400, "Excel 中没有可识别的商品行");
            }
            return String.join("\n", lines);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("parse excel failed", ex);
            throw BusinessException.of(500, "Excel 解析失败");
        } catch (Exception ex) {
            log.error("parse excel failed", ex);
            throw BusinessException.of(400, "Excel 格式无法解析，请检查文件内容");
        }
    }

    private String rowToText(Row row) {
        if (row == null) {
            return null;
        }
        List<String> cells = new ArrayList<>();
        int lastCell = row.getLastCellNum();
        for (int i = 0; i < lastCell; i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                continue;
            }
            String value = FORMATTER.formatCellValue(cell).trim();
            if (StringUtils.hasText(value)) {
                cells.add(value);
            }
        }
        if (cells.isEmpty()) {
            return null;
        }
        if (cells.size() >= 3) {
            return cells.get(0) + cells.get(1) + cells.get(2);
        }
        if (cells.size() == 2) {
            return cells.get(0) + cells.get(1);
        }
        return cells.get(0);
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
