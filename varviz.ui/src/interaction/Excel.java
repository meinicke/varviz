package interaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;

public class Excel {

	private HSSFWorkbook workbook;

	public void writesheet(Map<String, Object[]> table, File workingDir) throws IOException {

		workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("Interactions");
		HSSFSheet sheet2 = workbook.createSheet("Colors");
		Row row;
		Row row2;


		Set<String> keyid = table.keySet();
		int rowid = 0;
		for (String key : keyid) {
			row = sheet.createRow(rowid++);
			Object[] objArr = table.get(key);
			int cellid = 0;
			for (Object obj : objArr) {
				Cell cell = row.createCell(cellid++);
				cell.setCellValue((String) obj);
				// System.out.println((String)obj);
				cell.setCellStyle((CellStyle) setBackground(obj).get(0));
				sheet.autoSizeColumn(cellid - 1);
			}

		}

		// it creates a new sheet with colors only, without text
		rowid = 0;
		for (String key : keyid) {
			row2 = sheet2.createRow(rowid++);
			Object[] objArr = table.get(key);
			int cellid = 0;
			for (Object obj : objArr) {
				Cell cell = row2.createCell(cellid++);

				CellStyle style = (CellStyle) setBackground(obj).get(0);
				cell.setCellValue((String) setBackground(obj).get(1));
				// System.out.println((String)obj);
				cell.setCellStyle(style);
				sheet2.autoSizeColumn(cellid - 1);
			}

		}

		FileOutputStream out;
		try {
			File file = new File(workingDir.getAbsolutePath() + "/Interactions.xls");

			System.out.println("write to: " + file.getAbsolutePath());
			out = new FileOutputStream(file);
			workbook.write(out);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	private ArrayList<Object> setBackground(Object obj) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();

		if (obj.toString().contains("both")) {
			style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
			obj = " ";
		} else if (obj.toString().contains("suppresses")) {
			style.setFillForegroundColor(IndexedColors.RED.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
			obj = " ";
		} else if (obj.toString().contains("enables")) {
			style.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
			obj = " ";
		} else if (obj.toString().contains("effect")) {
			style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
			obj = " ";
		} else if (obj.toString().contains(" X ")) {
			style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		} else if (obj.toString().contains("not interact")) {
			style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
			obj = " ";
		} else if (obj.toString().contains("interact")) {
			style.setFillForegroundColor(IndexedColors.PINK.getIndex());
			style.setFillPattern(CellStyle.SOLID_FOREGROUND);
			obj = " ";
		} else {
			style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
			font.setBold(true);
			font.setFontHeightInPoints((short) 14);
			style.setFont(font);
		}

		ArrayList<Object> array = new ArrayList<>();
		array.add(style);
		array.add(obj);
		// style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());

		return array;
	}
}
