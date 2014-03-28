import java.io.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.fill.JRTemplatePrintText;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import au.com.bytecode.opencsv.CSVReader;

public class PdfWriter2 {

    public static void main(String[] args) {
        new PdfWriter2().start(args);
    }

    public void start(String[] args) {
        // ファイル名格納用変数
        String filename_datacsv1 = "";  // データCSVファイル1
        String filename_paramcsv1 = ""; // パラメータCSVファイル1
        String filename_templatejrxml1 = ""; // テンプレートJRXMLファイル1
        String filename_datacsv2 = "";  // データCSVファイル2
        String filename_paramcsv2 = ""; // パラメータCSVファイル2
        String filename_templatejrxml2 = ""; // テンプレートJRXMLファイル2
        String filename_pdf = "";      // 出力PDFファイル
        String key_for_merge = ""; // 結合用キー
        
        // コマンドライン引数を解析する
        Options opts = new Options();
        opts.addOption("t1", "templatejrxml", true, "input template jrxml file");
        opts.addOption("d1", "datacsv", true, "input data csv file");
        opts.addOption("p1", "paramcsv", true, "input parameter csv file");
        opts.addOption("t2", "templatejrxml", true, "input template jrxml file");
        opts.addOption("d2", "datacsv", true, "input data csv file");
        opts.addOption("p2", "paramcsv", true, "input parameter csv file");
        opts.addOption("k", "key_for_merge", true, "key for merge");
        opts.addOption("o", "outpdf", true, "output pdf file");
        BasicParser parser = new BasicParser();
        CommandLine cl;
        HelpFormatter help = new HelpFormatter();
        try {
            // 解析する
            cl = parser.parse(opts, args);
            
            filename_datacsv1 = cl.getOptionValue("d1");
            if(filename_datacsv1 == null) {
                throw new ParseException("");
            }
            filename_datacsv2 = cl.getOptionValue("d2");
            if(filename_datacsv2 == null) {
                throw new ParseException("");
            }

            filename_pdf = cl.getOptionValue("o");
            if(filename_pdf == null) {
                throw new ParseException("");                
            }
            key_for_merge = cl.getOptionValue("k");
            if(key_for_merge == null) {
                throw new ParseException("");                
            }
            
            filename_paramcsv1 = cl.getOptionValue("p1");
            if(filename_paramcsv1 == null) {
                throw new ParseException("");                                
            }
            filename_paramcsv2 = cl.getOptionValue("p2");
            if(filename_paramcsv2 == null) {
                throw new ParseException("");                                
            }
            
            filename_templatejrxml1 = cl.getOptionValue("t1");
            if(filename_templatejrxml1 == null) {
                throw new ParseException("");                                
            }
            filename_templatejrxml2 = cl.getOptionValue("t2");
            if(filename_templatejrxml2 == null) {
                throw new ParseException("");                                
            }
        }catch (ParseException e) {
            help.printHelp("PdfWriter2", opts);
            System.exit(1);
        }
        
        // パラメータCSVファイルからパラメータを読み込み
        // 1行目はヘッダとしてスキップする
        // 列の区切りは水平タブ
        // 行の区切りは改行
        // 2行目以降から1列目をキー2列目をバリューとして読み込む
        Map<String, Object> parameters1 = null;
        try {
        	parameters1 = this.parseParameterFile(filename_paramcsv1);
        } catch(Exception e) {
        	System.out.println("error in parsing paramcsv1");
        	System.out.println(e.toString());
            System.exit(1);
        }
        
        Map<String, Object> parameters2 = null;
        try {
        	parameters2 = this.parseParameterFile(filename_paramcsv2);
        } catch(Exception e) {
        	System.out.println("error in parsing paramcsv2");
        	System.out.println(e.toString());
            System.exit(1);
        }
        
        // JasperReportでPDFを生成する
        try {
            // .jrxmlを読み込んでコンパイルする
            JasperReport jasperReport1 = JasperCompileManager.compileReport(filename_templatejrxml1);
            JasperReport jasperReport2 = JasperCompileManager.compileReport(filename_templatejrxml2);
            
            // データCSVファイルをデータソースに設定する
            // 1行目はヘッダとしてスキップ
            // 列の区切りは水平タブ
            // 行の区切りはLF
            JRCsvDataSource ds1 = new JRCsvDataSource(filename_datacsv1, "UTF-8");
            ds1.setUseFirstRowAsHeader(true);
            ds1.setFieldDelimiter(',');
            ds1.setRecordDelimiter("\n");
            JRCsvDataSource ds2 = new JRCsvDataSource(filename_datacsv2, "UTF-8");
            ds2.setUseFirstRowAsHeader(true);
            ds2.setFieldDelimiter(',');
            ds2.setRecordDelimiter("\n");
            
            // ドキュメントを生成
            JasperPrint jasperPrint1 = JasperFillManager.fillReport(jasperReport1, parameters1, ds1);
            JasperPrint jasperPrint2 = JasperFillManager.fillReport(jasperReport2, parameters2, ds2);
            
            // 1つ目に2つ目をマージしていく
            // 1つ目と2つ目は1対多を想定
            // マージの基準はTemplatePlainTextエレメントのKey属性の値がkey_for_mergeのテキストが一致するものを基準に
            // 1つ目のページの後に2つ目のページを挿入する
            List<JRPrintPage> pages1 = jasperPrint1.getPages();
            List<JRPrintPage> pages2 = jasperPrint2.getPages();
            List<JRPrintPage> pages1_orig = new ArrayList<JRPrintPage>();
            for (JRPrintPage page1 : pages1) {
            	pages1_orig.add(page1);
            }
            JRPrintPage page2_prev = null;
            JRTemplatePrintText pt2_prev = null;
            int n1 = 0;
            Iterator<JRPrintPage> i2 = pages2.iterator();
            for (JRPrintPage page1 : pages1_orig) {
            	n1 += 1;
            	// Key属性の値がkey_for_mergeと一致するTemplatePrintTextエレメントを取得
            	JRTemplatePrintText pt1 = this.getTemplatePrintTextByKey(page1, key_for_merge);
            	if(pt1 == null) {
                	System.out.println("error in processing jrxml1, no key element in page");            		
                    System.exit(1);
            	}
            	// 2つ目の前回ページのTemplatePrintTextエレメントを取得済みの場合
            	if(pt2_prev != null) {
            		// TemplatePrintTextエレメンのテキストで比較する
            		if(!(((String)pt1.getValue()).equals((String)pt2_prev.getValue()))) {
            			continue;
            		}
            		// ページを追加
            		jasperPrint1.addPage(n1, page2_prev);
            		n1 += 1;
            		page2_prev = null;
            		pt2_prev = null;
            	}
            	while(i2.hasNext()) {
            		JRPrintPage page2 = (JRPrintPage)i2.next();
                	// Key属性の値がkey_for_mergeと一致するTemplatePrintTextエレメントを取得
            		JRTemplatePrintText pt2 = this.getTemplatePrintTextByKey(page2, key_for_merge);
                    if(pt2 == null) {
                        System.out.println("error in processing jrxml2, no key element in page");            		
                        System.exit(1);
                    }
            		// TemplatePrintTextエレメンのテキストで比較する
                    if(!(((String)pt1.getValue()).equals((String)pt2.getValue()))) { 
                    	page2_prev = page2;
                    	pt2_prev = pt2;
                    	break;
                    }
            		// ページを追加
            		jasperPrint1.addPage(n1, page2);
            		n1 += 1;
            	}
            }
            
            // PDFファイルを出力
            JasperExportManager.exportReportToPdfFile(jasperPrint1, filename_pdf);
        } catch (Exception e) {
        	System.out.println("error in exporting pdf");
        	System.out.println(e.toString());
            System.exit(1);
        }
    }
        
    private JRTemplatePrintText getTemplatePrintTextByKey(JRPrintPage page, String key) {
    	List<JRPrintElement> elms = page.getElements();
    	JRTemplatePrintText pt = null;
    	for(JRPrintElement elm : elms) {
    		if(!(elm instanceof JRTemplatePrintText)) {
    			continue;
    		}
    		if(elm.getKey() == null) {
    			continue;
    		}
    		if(!elm.getKey().equals(key)) {
    			continue;
    		}
    		pt = (JRTemplatePrintText)elm;
    		break;
    	}
    	return pt;
    }
    
    private Map<String, Object> parseParameterFile(String filename) throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        try {
        	FileInputStream input=new FileInputStream(filename);
        	InputStreamReader inReader=new InputStreamReader(input, "UTF-8");
        	CSVReader reader = new CSVReader(inReader,',','"',1);
        	String [] nextLine;
        	while ((nextLine = reader.readNext()) != null) {
        		if(nextLine.length >= 2) {
        			parameters.put(nextLine[0], nextLine[1]);
        		}
        	}
        	reader.close();
        } catch(Exception e) {
        	throw e;
        }
        return parameters;
    }
}
