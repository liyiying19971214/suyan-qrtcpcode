package com.example.suyanqrtcpcode.core.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.example.suyanqrtcpcode.core.service.StorageService;
import com.example.suyanqrtcpcode.exceptions.BusinessException;
import com.example.suyanqrtcpcode.runner.LoadDataRunner;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;



@Service
public class StorageServiceImpl implements StorageService {
	private static final Logger logger= LoggerFactory.getLogger(StorageServiceImpl.class);
	private static Map<String,String[]> excelRecordMap=new HashMap<String,String[]>() ;
	private static Map<String,String[]> notSaveRecordMap=new HashMap<String,String[]>();
	@Value("${data_xls_path}")
	private String xlsPath;

	@Override
	public void save2MyLog(JSONObject jo) throws Exception{


		LoadDataRunner.setScanDataMap(jo,true);
	}




	//目前未完善后续有需要再改
	@Override
	public void save2Xls(String bucketId, String QRCodeStr) throws Exception{
		InputStream inputStream=null;
		OutputStream outputStream=null;
		Workbook workbook=null;
		Date today=new Date();
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timeStr=sdf1.format(today);
		try{
			File xlsDirPathFile=new File(xlsPath);
	  		if(!xlsDirPathFile.exists() && !xlsDirPathFile.isDirectory()){
				xlsDirPathFile.mkdir();
			}
  	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

	 		String xlsPathStr=xlsDirPathFile+File.separator+"QRDATA_"+sdf.format(today)+".xlsx";
 	  		File xlsFile=new File(xlsPathStr);
	        if(xlsFile.exists()){  //如果文件存在
	        	inputStream = new FileInputStream(xlsFile) ;
	        	 if (xlsPathStr.endsWith(".xls")) {
		            	workbook = new HSSFWorkbook(inputStream);
		            } else if (xlsPathStr.endsWith(".xlsx")) {
		            	workbook = new XSSFWorkbook(inputStream);
		            }

	        }else {  //如果目标文件不存在
	            if (xlsPathStr.endsWith(".xls")) {
	            	workbook = new HSSFWorkbook();
	            } else if (xlsPathStr.endsWith(".xlsx")) {
	            	workbook = new XSSFWorkbook();
	            }
	        }
			Sheet sheet=workbook.getSheet("水桶打印记录");

			if(sheet==null){
				sheet=workbook.createSheet("水桶打印记录");
				sheet.setDefaultColumnWidth(50);
				Row rowTitle=sheet.createRow(0);
				rowTitle.createCell(0).setCellValue("水桶ID");
				rowTitle.createCell(1).setCellValue("二维码地址");
				rowTitle.createCell(2).setCellValue("打印时间");
			}
			int nextRowNum=sheet.getLastRowNum()+1;
			Row row=sheet.createRow(nextRowNum);
			row.createCell(0).setCellValue(bucketId);
			row.createCell(1).setCellValue(QRCodeStr);
			row.createCell(2).setCellValue(bucketId);

			row.createCell(2).setCellValue(timeStr);
			outputStream = new FileOutputStream(xlsFile);   //获取文件流
			outputStream.flush();
            workbook.write(outputStream);   //将workbook写入文件流
            excelRecordMap.put(bucketId, new String[]{bucketId,QRCodeStr,timeStr});

		}catch(Exception e){
			notSaveRecordMap.put(bucketId, new String[]{bucketId,QRCodeStr,timeStr});
			throw new BusinessException(e.getMessage());
		}finally{
			closeAll( workbook,inputStream,outputStream);

		}

	}
/*	   private static Workbook createNewWorkbookIfNotExist(String fileName) throws Exception {
	        Workbook wb = null;
	        if(fileName.endsWith(".xls")) {
	            wb = new HSSFWorkbook();
	        } else if(fileName.endsWith(".xlsx")) {
	            wb = new XSSFWorkbook();
	        } else {
	            throw new Exception("文件类型错误！既不是.xls也不是.xlsx");
	        }

	        try{
	            OutputStream output = new FileOutputStream(fileName);
	            wb.write(output);
	        }catch(FileNotFoundException e) {
	            System.out.println("文件创建失败，失败原因为：" + e.getMessage());
	            throw new FileNotFoundException();
	        }
	        System.out.println(fileName + "文件创建成功！");

	        return wb;
	    }
	   private static Workbook createWorkbook(String fileName) throws Exception {
	        InputStream input = null;
	        Workbook wb = null;

	        try{
	            input = new FileInputStream(fileName);
	            wb = WorkbookFactory.create(input);
	            if (!new File(fileName).exists()){  //如果不存在
	                wb = createNewWorkbookIfNotExist(fileName);   //创建新的
	            }
	        } catch(OldExcelFormatException e) {
	            System.out.println("文件打开失败，原因：要打开的Excel文件版本过低！");
	            throw new OldExcelFormatException("文件版本过低");
	        } finally {
	            if(input != null) {
	                input.close();
	            }
	        }
	        return wb;
	    }*/
	private void closeAll(Workbook workbook,InputStream inputStream,OutputStream outputStream){
		if(workbook!=null){
			try {
				workbook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(inputStream!=null){
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(outputStream!=null){
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
