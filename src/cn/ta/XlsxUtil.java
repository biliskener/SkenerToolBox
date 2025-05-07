package cn.ta;

import com.strobel.core.StringUtilities;
import org.apache.poi.ss.usermodel.*;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class XlsxUtil {
    public static final int TABLE_TYPE_ROW_INDEX = 0;
    public static final int TABLE_TYPE_COL_INDEX = 0;
    public static final int TABLE_TYPE_COL_COUNT = 3;
    public static final int DESC_ROW_INDEX = 1;
    public static final int NAME_ROW_INDEX = 2;
    public static final int TYPE_ROW_INDEX = 3;
    public static final int EXT_ROW_INDEX = 4;
    public static final int DATA_FIRST_ROW_INDEX = 5;

    static String loadCellValue(Cell cell) throws UnexpectedException {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            // 必须要转，不然会变成该死的浮点数
            cell.setCellType(CellType.STRING);
            String numberValue = cell.getStringCellValue();
            return numberValue;
        } else if (cell.getCellType() == CellType.BLANK) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.FORMULA) {
            cell.setCellType(CellType.STRING);
            String numberValue = cell.getStringCellValue();
            return numberValue;
        } else {
            throw new UnexpectedException("Unknown cell type: " + cell.getCellType());
        }
    }

    static ArrayList<String> loadRowValue(Sheet sheet, int rowIndex, int colCount, Set<Integer> ignoringColSet) throws UnexpectedException {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            ArrayList<String> rowData = new ArrayList<>();
            for (int colIndex = 0; colCount <= 0 || colIndex < colCount; ++colIndex) {
                if(ignoringColSet == null || ignoringColSet.contains(colIndex) == false) {
                    Cell cell = row.getCell(colIndex);
                    if (cell != null) {
                        String cellValue = loadCellValue(cell);
                        if(rowIndex == DESC_ROW_INDEX) {
                            Comment cellComment = cell.getCellComment();
                            if(cellComment != null) {
                                String comment = cellComment.getString().getString().trim();
                                if(comment.length() > 0) {
                                    cellValue = cellValue + "(" + comment + ")";
                                }
                            }
                        }
                        rowData.add(cellValue);
                    }
                    else if(colCount > 0) {
                        rowData.add("");
                    }
                    else {
                        break;
                    }
                }
            }
            return rowData;
        } else if (rowIndex == EXT_ROW_INDEX) {
            ArrayList<String> rowData = new ArrayList<>();
            for (int colIndex = 0; colCount <= 0 || colIndex < colCount; ++colIndex) {
                if(ignoringColSet == null || ignoringColSet.contains(colIndex) == false) {
                    rowData.add("");
                }
            }
            return rowData;
        } else {
            return null;
        }
    }

    static List<List<String>> loadTableData(Sheet sheet) throws UnexpectedException {
        List<List<String>> tableData = new ArrayList<>();

        // 取出行数和列数
        int rowCount = sheet.getLastRowNum() + 1;

        // 取出表头
        List<String> tableRow = loadRowValue(sheet, TABLE_TYPE_ROW_INDEX, TABLE_TYPE_COL_COUNT, null);
        assert(tableRow != null);

        // 字段名行
        ArrayList<String> nameRow = loadRowValue(sheet, NAME_ROW_INDEX, 0, null);
        assert(nameRow != null);

        // 删除空字段开始的后面所有字段
        for(int colIndex = 0; colIndex < nameRow.size(); ++colIndex) {
            String name = nameRow.get(colIndex);
            if(name.matches("^\\s*$")) {
                while(colIndex < nameRow.size()) {
                    nameRow.remove(colIndex);
                }
            }
        }

        // 最大列数(包括#开头的)
        int colCount = nameRow.size();

        ArrayList<String> descRow = loadRowValue(sheet, DESC_ROW_INDEX, colCount, null);
        assert(descRow != null);
        assert(descRow.size() == nameRow.size());

        // 找出所有#开头的列, 加入忽略列表
        Set<Integer> ignoringColSet = new TreeSet<>();
        for(int colIndex = descRow.size() - 1; colIndex >= 0; --colIndex) {
            String desc = descRow.get(colIndex);
            String name = nameRow.get(colIndex);
            if(desc.matches("^\\s*#.*$") || name.matches("^\\s*#.*$")) {
                nameRow.remove(colIndex);
                descRow.remove(colIndex);
                ignoringColSet.add(colIndex);
            }
        }

        ArrayList<String> typeRow = loadRowValue(sheet, TYPE_ROW_INDEX, colCount, ignoringColSet);
        assert(typeRow != null);
        assert(typeRow.size() == descRow.size());

        ArrayList<String> extRow = loadRowValue(sheet, EXT_ROW_INDEX, colCount, ignoringColSet);
        assert(extRow != null);
        assert(extRow.size() == descRow.size());

        tableData.add(tableRow);
        tableData.add(descRow);
        tableData.add(nameRow);
        tableData.add(typeRow);
        tableData.add(extRow);

        for(int rowIndex = DATA_FIRST_ROW_INDEX; rowIndex < rowCount; ++rowIndex) {
            ArrayList<String> rowData = loadRowValue(sheet, rowIndex, colCount, ignoringColSet);
            if(rowData != null) {
                if(rowData.size() > 0 && (rowData.get(0).startsWith("#") || rowData.get(0).matches("^\\s*$"))) {
                    System.out.println("    row ignored: " + rowIndex + " data: " + StringUtilities.join("|", rowData));
                }
                else {
                    assert(rowData.size() == descRow.size());
                    tableData.add(rowData);
                }
            }
        }

        return tableData;
    }

    static List<List<String>> revertTableData(List<List<String>> tableData) {
        return tableData;
    }
}
