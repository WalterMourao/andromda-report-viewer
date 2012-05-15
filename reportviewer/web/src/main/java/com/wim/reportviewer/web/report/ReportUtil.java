package com.wim.reportviewer.web.report;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

import com.wim.reportviewer.BeanLocator;
import com.wim.reportviewer.web.WebAppUtil;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuterFactory;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

public class ReportUtil {

    public static Map<String,String> getParameterProperties(JRParameter jrParameter){
		Map<String,String> propriedades = new HashMap<String,String>();

		if(jrParameter.getPropertiesMap()!=null){

			for (final String propriedade : jrParameter.getPropertiesMap().getPropertyNames()){
				propriedades.put(propriedade, jrParameter.getPropertiesMap().getProperty(propriedade));
			}

		}

		return propriedades;
	}    

	public static String removeAccents(String acentuada) {
		return Normalizer.normalize(acentuada, Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}  

	@SuppressWarnings("unchecked")
	public static Map<String,JasperDesign> getReports() throws IOException{
	    final ServletContext context=WebAppUtil.getServletContext();
	    Map<String,JasperDesign> result=(Map<String,JasperDesign>)context.getAttribute(ReportConstants.LIST_ALL_REPORTS_JASPER);
	    if(result == null){
	        result=compileReports();
	        context.setAttribute(ReportConstants.LIST_ALL_REPORTS_JASPER, result);
	    }
		return result;
	}

	public static boolean isReportLink(final String isLinked){
		return (isLinked!=null && Boolean.valueOf(isLinked));
	}

	private static Map<String,JasperDesign> compileReports(String sourceFolder, String destFolder, final boolean force) throws IOException{
        final Map<String,JasperDesign> relatoriosCompilados = new Hashtable<String,JasperDesign>();
	    
        final File[] files = new File(sourceFolder).listFiles((FilenameFilter)FileFilterUtils.suffixFileFilter(".jrxml"));
        
        if (!ArrayUtils.isEmpty(files)) {
            FileUtils.forceMkdir(new File(destFolder));
            for (File file : files) {
                try {
                    final JasperDesign report = JRXmlLoader.load(new FileInputStream(file));
                    final String baseFileName=FilenameUtils.removeExtension(file.getName());
                    //compila os relatorios verificando se o jrxml é mais novo que o jasper ou se force=true
                    final String nomeRelatorioCompilado=FilenameUtils.concat(destFolder ,baseFileName  + ReportConstants.JASPER_COMPILED_EXTENSION);
                    final File relatorioCompilado = new File(nomeRelatorioCompilado);
                    if(   (!relatorioCompilado.exists()) || (file.lastModified() > relatorioCompilado.lastModified()) || force ){
                        JasperCompileManager.compileReportToFile(report, relatorioCompilado.getAbsolutePath());
                    }

                    relatoriosCompilados.put(baseFileName, report);

                } catch (Exception e) {
                    throw new RuntimeException("Falha na leitura ou compilação do relatorio " + file.getName() + " " +e.getLocalizedMessage()); 
                }
            }
        }
        
        return relatoriosCompilados;
	}

	public static Map<String,JasperDesign> compileReports() throws IOException{
		final Map<String,JasperDesign> lstRelatoriosEstruturados;

		//compilando os relatórios do previne
		final ServletContext context=WebAppUtil.getServletContext();
		final String sourcePath =  context.getRealPath(ReportConstants.REPORTS_FOLDER_SOURCE);
		final String destPath = context.getRealPath(ReportConstants.REPORTS_FOLDER_COMPILED);
		lstRelatoriosEstruturados = compileReports(sourcePath,destPath,false);
		
		return lstRelatoriosEstruturados;
	}

	@SuppressWarnings("unchecked")
	public static JasperDesign loadReport(String fileName){
		final Hashtable<String,JasperDesign> lstReports = (Hashtable<String,JasperDesign>)((HttpServletRequest)WebAppUtil.getFacesContext().getExternalContext().getRequest()).getSession().getServletContext().getAttribute(ReportConstants.LIST_ALL_REPORTS_JASPER);
		return lstReports.get(fileName);
	} 

	private static Session getCurrentHibernateSession(){
	    return SessionFactoryUtils.getSession((SessionFactory)BeanLocator.instance().getBean("sessionFactory"), true);
	}
	
	/* jasperFileName é o nome do relatório com ou sem a extensão .jrxml, vai buscar o .jasper do disco */
	@SuppressWarnings("deprecation")
	public static JasperPrint generateReport(String jasperFileName,Map<String, Object> parameters) throws Exception {
		final String compiledReportFolder=WebAppUtil.getServletContext().getRealPath(ReportConstants.REPORTS_FOLDER_COMPILED)+ File.separator;
		parameters.put("SUBREPORT_DIR",compiledReportFolder);

		final Session session = getCurrentHibernateSession();
        parameters.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session );
        
		try{
    		parameters.put(JRParameter.REPORT_CONNECTION, session.connection() );
    		JasperPrint jasperPrint = JasperFillManager.fillReport(compiledReportFolder+FilenameUtils.getBaseName(jasperFileName)+ReportConstants.JASPER_COMPILED_EXTENSION, parameters);
            return jasperPrint;
		}
		finally{
		    session.close();
		}
	}

	/* correções necessárias antes de enviar os parâmetros */
	public static void checkParameters(Map<String, Object> valoresParametros,
			JRParameter[] jrParameters) {
		//converterndo os parametros diferentes de todos para vazios para null
		for(String param:valoresParametros.keySet()) {

			Object value = valoresParametros.get(param);
			//Quando se usa relatorio linkado os valores dos paramentros vem em um array de String.
			if(value instanceof String[])
				value = ((String[])value)[0];

			if(value instanceof String && StringUtils.isEmpty((String)value)){// o único caso de um tipo não ser nulo e ser vazio é com String
				valoresParametros.put(param, null);
				value = null;
			}
		}

		//conferindo se os tipos estão certos (gambi para o autocomplete,para parametros recebidos no request e outras conversões)
		for(JRParameter jrp:jrParameters ){
			Object value=valoresParametros.get(jrp.getName());
			if(value != null){
				if(value instanceof String[])
					value = ((String[])value)[0];

				try {
					if(!jrp.getValueClass().equals(value.getClass())){
						
						if (jrp.getPropertiesMap().getProperty("useLike")!=null && jrp.getPropertiesMap().getProperty("useLike").toString().equalsIgnoreCase("true")){
						value = ("%" + value + "%");
						}
						else if (jrp.getPropertiesMap().getProperty("useLike")!=null && jrp.getPropertiesMap().getProperty("useLike").toString().equalsIgnoreCase("left")){
							value = ("%" + value);
						}
						else if (jrp.getPropertiesMap().getProperty("useLike")!=null && jrp.getPropertiesMap().getProperty("useLike").toString().equalsIgnoreCase("right")){
							value = (value + "%");
							}
						if(jrp.getValueClass().equals(Timestamp.class)){
							if(value.getClass().equals(Date.class)){ //Date para timestamp
								valoresParametros.put(jrp.getName(), new Timestamp(((Date)value).getTime()));
							} else if (value.getClass().equals(String.class)){ //String para timestamp
								valoresParametros.put(jrp.getName(), new Timestamp(java.text.DateFormat.getDateInstance().parse(value.toString()).getTime()));
							} else {
								throw new RuntimeException("Conversão não suportada");
							}
						} else {
							value=jrp.getValueClass().getMethod("valueOf", String.class).invoke(null,value.toString());
							valoresParametros.put(jrp.getName(),value);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Falha convertendo de "+value.getClass().getName()+" para "+jrp.getValueClass().getName(),e);
				}
			}
		}
	}

	

	public static ReportPrintInfo prepareReport(String fileName,
			Map<String,Object> valoresParametros) {

		final JasperDesign design =  ReportUtil.loadReport(fileName);
		final JRParameter[] jrParameters = design.getParameters();
		
		checkParameters(valoresParametros,jrParameters);

		final JasperPrint jasperPrint;
		try {
			jasperPrint = ReportUtil.generateReport(fileName,  valoresParametros);
		} catch (Exception e) {
			throw new RuntimeException("manutencao.de.relatorios.relatorio.relatorio.falha.geracao: "+e.getLocalizedMessage(),e);
		}

		return new ReportPrintInfo(jasperPrint,design,valoresParametros);
	}

	
	public static void exportReport(final ReportPrintInfo printInfo, final HttpServletResponse response){
		final JasperPrint jasperPrint=printInfo.getJasperPrint();

		final StringBuffer downloadFileName=new StringBuffer(jasperPrint.getName());

		response.setContentType(ReportConstants.MIME_TYPE_PDF);
		try {
            response.getOutputStream().write(JasperExportManager.exportReportToPdf(jasperPrint));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
		downloadFileName.append(ReportConstants.PDF_EXTENSION);

		response.setHeader("Content-disposition", "attachment; filename=" + downloadFileName );
		WebAppUtil.getFacesContext().responseComplete();
	}

	public static String generateHtmlReportPage(ReportPrintInfo printInfo, int page) throws JRException, IOException{        
	    final JasperPrint jasperPrint=printInfo.getJasperPrint();
	    
		if(page < 0){
			page = 0;
		} else if(page >= jasperPrint.getPages().size()){
			page = jasperPrint.getPages().size()-1;
		} else {
			page -= 1;
		}

		final StringWriter result=new StringWriter();
		final JRHtmlExporter exporter = new JRHtmlExporter();

		final Map<String,Object> imagesMap = new HashMap<String,Object>();
		printInfo.setImagesMap(imagesMap);
		
		//should use pageflowscope
		WebAppUtil.getSession().setAttribute(ReportConstants.IMAGES_MAP, imagesMap);
		
		exporter.setParameter(JRHtmlExporterParameter.ZOOM_RATIO, new Float(1.5));
		exporter.setParameter(JRExporterParameter.START_PAGE_INDEX, page);
		exporter.setParameter(JRExporterParameter.END_PAGE_INDEX, page);
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.CHARACTER_ENCODING, "ISO8859_1");
		exporter.setParameter(JRHtmlExporterParameter.IMAGES_MAP, imagesMap);
		exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, WebAppUtil.getRequest().getContextPath() + ReportConstants.REPORT_IMAGE_SERVLET  + "?image=");
		exporter.setParameter(JRExporterParameter.OUTPUT_WRITER, result);
		exporter.exportReport();

		return result.toString();
	}    

	public static String generateHtmlBodyReportPage(ReportPrintInfo printInfo, int page) throws JRException, IOException{
		return StringUtils.substringBetween(
				StringUtils.substringAfter(generateHtmlReportPage(printInfo, page),"<body"),
				">","</body");
	}

}