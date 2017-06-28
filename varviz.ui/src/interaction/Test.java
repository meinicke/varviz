package interaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Test {
	
//	public Test() {
//        super();
//    }

	public static void main(String[] args) throws IOException {
//			Test helloPOI = new Test();
//	        helloPOI.geraPlanilha();
//	        System.out.println("fim da aplicacao");
		
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet    sheet    = workbook.createSheet("ListaEmpl");
		//Row row = sheet.createRow((short)0);
		Row row;
	    //Cell cell = row.createCell((short) 0);
		//cell.setCellValue("Have a Cup of XL");
		
		  Map < String, Object[] > empinfo = new TreeMap < String, Object[] >();
	        empinfo.put( "1", new Object[] { "blocking", "Paralell", "Parallel has no effect" });
	        empinfo.put( "2", new Object[] { "tp01", "Gopal", "Technical Manager" });
	        empinfo.put( "3", new Object[] { "tp02", "Manisha", "Proof Reader" });
	        empinfo.put( "4", new Object[] { "tp03", "Masthan", "Technical Writer" });
	        empinfo.put( "5", new Object[] { "tp04", "Satish", "Technical Writer" });
	        empinfo.put( "6", new Object[] { "tp05", "Krishna", "Technical Writer" });
	                
	        Set <String> keyid = empinfo.keySet();
	        int rowid = 0;
	        for (String key: keyid){
	            row = sheet.createRow(rowid++);
	            Object[] objArr = empinfo.get(key); //pega cada item da linha e coloca tipo num vetor
	            int cellid = 0;
	            for (Object obj : objArr){//pega o primeiro item da linha
	                Cell cell = row.createCell(cellid++);
	                cell.setCellValue((String)obj);
	                System.out.println((String)obj);
	            }
	        }
		 
		 
		 FileOutputStream out;
		try {
			out = new FileOutputStream(new File("PlanilhaPoi.xls"));
			workbook.write(out);
	        out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
	}
	
public void geraPlanilha(){
        
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet    sheet    = workbook.createSheet("ListaEmpl");
        XSSFRow      row;
        
        Map < String, Object[] > empinfo = new TreeMap < String, Object[] >();
        empinfo.put( "1", new Object[] { "EMP ID", "EMP NAME", "DESIGNATION" });
        empinfo.put( "2", new Object[] { "tp01", "Gopal", "Technical Manager" });
        empinfo.put( "3", new Object[] { "tp02", "Manisha", "Proof Reader" });
        empinfo.put( "4", new Object[] { "tp03", "Masthan", "Technical Writer" });
        empinfo.put( "5", new Object[] { "tp04", "Satish", "Technical Writer" });
        empinfo.put( "6", new Object[] { "tp05", "Krishna", "Technical Writer" });
                
        Set <String> keyid = empinfo.keySet();
        int rowid = 0;
        for (String key: keyid){
            row = sheet.createRow(rowid++);
            Object[] objArr = empinfo.get(key);
            int cellid = 0;
            for (Object obj : objArr){
                Cell cell = row.createCell(cellid++);
                cell.setCellValue((String)obj);
                System.out.println((String)obj);
            }
        }
        
        try{
            FileOutputStream out = new FileOutputStream(new File("PlanilhaPoi.xlsx"));
            workbook.write(out);
            out.close();
        } catch( FileNotFoundException e){
            System.out.println("ERRO: Arquino nao encontrado");
            } catch (IOException e){
                System.out.println("ERRO: na escrita da planilha");
            }
        System.out.println("terminei de escrever");
    }

}
