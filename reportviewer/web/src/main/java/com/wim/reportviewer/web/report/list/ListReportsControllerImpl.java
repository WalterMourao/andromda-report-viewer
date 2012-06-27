// license-header java merge-point
// Generated by andromda-jsf cartridge (controllers\ControllerImpl.java.vsl) on 05/11/2012 10:26:51-0300
package com.wim.reportviewer.web.report.list;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.convert.Converter;
import javax.faces.convert.DateTimeConverter;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.design.JasperDesign;

import org.andromda.presentation.jsf.Messages;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.wim.reportviewer.web.WebAppUtil;
import com.wim.reportviewer.web.report.ReportConstants;
import com.wim.reportviewer.web.report.ReportInfo;
import com.wim.reportviewer.web.report.ReportParameterInfo;
import com.wim.reportviewer.web.report.ReportPrintInfo;
import com.wim.reportviewer.web.report.ReportUtil;

/**
 * @see com.wim.reportviewer.web.report.list.ListReportsController
 */
public class ListReportsControllerImpl
    extends ListReportsController
{
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1306594233538629786L;

    private static final String CURRENT_PRINT_INFO = ListReportsControllerImpl.class.getCanonicalName()+"_CURRENT_PRINT_INFO";
    
    private ReportPrintInfo getCurrentReportPrintInfo(){
        return (ReportPrintInfo)WebAppUtil.getPageFlowScope().get(CURRENT_PRINT_INFO);
    }
    
    private void setCurrentReportPrintInfo(ReportPrintInfo printInfo){
        WebAppUtil.getPageFlowScope().put(CURRENT_PRINT_INFO, printInfo);
    }
    
    private static final String SAVED_SEARCH = ListReportsControllerImpl.class.getCanonicalName()+"_SAVED_SEARCH";

    private Collection<ReportInfo> loadReports(String filter) throws IOException {
        final List<ReportInfo> reports = new java.util.ArrayList<ReportInfo>();
        final Map<String,JasperDesign> lstReports = ReportUtil.getReports();
        filter=ReportUtil.removeAccents(StringUtils.trimToEmpty(filter)).toLowerCase();

        final Iterator<String> it = lstReports.keySet().iterator();
        while (it.hasNext()) {
            final String currentReportFileName = (String) it.next();
            JasperDesign report = (JasperDesign)lstReports.get(currentReportFileName);

            ReportInfo ir = new ReportInfo();
            ir.setFileName(currentReportFileName);
            ir.setName(report.getName());

            if(StringUtils.isBlank(filter)){
                reports.add(ir);
            }else{
                if( ReportUtil.removeAccents(ir.getName()).toLowerCase().contains(filter)){
                    reports.add(ir);
                }
            }
        }

        Collections.sort(reports, new Comparator<ReportInfo>(){
            @Override
            public int compare(ReportInfo ri1, ReportInfo ri2) {
                return ri1.getName().compareToIgnoreCase(ri2.getName());
            }
        });
        
        return reports;
    }

    /**
     * @throws IOException 
     * @see com.wim.reportviewer.web.report.list.ListReportsController#refresh(java.util.Collection<com.wim.reportviewer.web.report.ReportInfo> reports)
     */
    @Override
    public void refresh(RefreshForm form) throws IOException
    {
        ReportUtil.compileReports();

        form.setReports(loadReports(null));
    }

    /**
     * @throws IOException 
     * @see com.wim.reportviewer.web.report.list.ListReportsController#initialize(java.lang.String fileName, java.util.Collection<com.wim.reportviewer.web.report.ReportInfo> reports)
     */
    @Override
    public void initialize(InitializeForm form) throws IOException
    {
        WebAppUtil.getPageFlowScope().remove(SAVED_SEARCH);
        form.setFileName(null);
        form.setReports(loadReports(null));
    }

    /**
     * @throws IOException 
     * @see com.wim.reportviewer.web.report.list.ListReportsController#search(java.lang.String report, java.util.Collection<com.wim.reportviewer.web.report.ReportInfo> reports)
     */
    @Override
    public void search(SearchForm form) throws IOException
    {
        String filter = StringUtils.trimToNull((String)WebAppUtil.getPageFlowScope().get(SAVED_SEARCH));
        form.setReports(loadReports(filter));
        form.setReport(filter);//pode estar vindo da visualização
    }

    public boolean isCurrentReportEmpty(){
        final ReportPrintInfo printInfo=getCurrentReportPrintInfo();
        return printInfo == null || printInfo.getJasperPrint().getPages().isEmpty();
    }

    /**
     * @see com.wim.reportviewer.web.report.list.ListReportsController#reportHasParameters(java.util.Collection<com.wim.reportviewer.web.report.ReportParameter> parameters)
     */
    @Override
    public boolean reportHasParameters(ReportHasParametersForm form)
    {
        return !CollectionUtils.isEmpty(form.getParameters());
    }

    /**
     * @see com.wim.reportviewer.web.report.list.ListReportsController#prepareParameters(java.lang.String fileName, java.util.Map parametersValues, java.lang.String title, java.lang.String parametersPage, java.util.Collection<com.wim.reportviewer.web.report.ReportParameter> parameters)
     */
    @Override
    public void prepareParameters(PrepareParametersForm form)
    {
        WebAppUtil.getPageFlowScope().remove(CURRENT_PRINT_INFO);

        final Map<String, Object> parametersValues = new java.util.HashMap<String, Object>();
        parametersValues.put(ReportConstants.REPORTS_BASE_DIR_VAR, super.getRequest().getSession().getServletContext().getRealPath(ReportConstants.REPORTS_FOLDER_COMPILED) + "/");

        final JasperDesign design =  ReportUtil.loadReport(form.getFileName());

        form.setTitle(design.getName());
        final JRParameter[] jrParameters = design.getParameters();
        final Collection<ReportParameterInfo> infoParameters = new ArrayList<ReportParameterInfo>();

        for (JRParameter jrParameter : jrParameters) {
            final String parameterName = jrParameter.getName();

            //Visiveis
            if (!jrParameter.isSystemDefined()) {
                if (jrParameter.isForPrompting()) {

                    final Converter converter;
                    if (jrParameter.getValueClass().equals(Date.class) || jrParameter.getValueClass().equals(Timestamp.class) || jrParameter.getValueClass().equals(java.sql.Date.class)) {
                        converter = new DateTimeConverter();
                    } else if (jrParameter.getValueClass().equals(java.lang.String.class)) {
                        converter = null;
                    } else {
                        final String className = "javax.faces.convert."+jrParameter.getValueClass().getSimpleName()+"Converter";
                        try {
                            converter = (Converter)Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    final ReportParameterInfo pInfo = new ReportParameterInfo();
                    pInfo.setName(parameterName);
                    pInfo.setLabel(jrParameter.getDescription()==null?parameterName:jrParameter.getDescription());
                    pInfo.setConverter(converter);
                    pInfo.setType(jrParameter.getValueClassName().toString());
                    pInfo.setProperties(ReportUtil.getParameterProperties(jrParameter));

                    infoParameters.add(pInfo);
                } 
            }
        }

        //Atribuindo valor nulo os parametros por default para os diferntes de 'Tods'
        for (final ReportParameterInfo info: infoParameters){
            if(!parametersValues.containsKey(info.getName())){
                parametersValues.put(info.getName() , null);
            }
        }

        form.setParameters(infoParameters);
        form.setParametersValues(parametersValues);

        final String pageParameterName = ReportConstants.REPORTS_PAGE_PARAMETERS + form.getFileName() + ".xhtml";
        final String path = super.getSession(false).getServletContext().getRealPath(pageParameterName);
        final File pageParameterFile = new File(path);
        if(pageParameterFile.exists())
            form.setParametersPage(pageParameterName);
        else
            form.setParametersPage(ReportConstants.REPORTS_PAGE_PARAMETERS + "_dynamic.xhtml");
    }

    /**
     * @see com.wim.reportviewer.web.report.list.ListReportsController#prepareVisualization(int currentPage, java.util.Map parametersValues, java.lang.String fileName)
     */
    @Override
    public void prepareVisualization(PrepareVisualizationForm form)
    {
        @SuppressWarnings("unchecked")
        final ReportPrintInfo printInfo=ReportUtil.prepareReport(form.getFileName(),form.getParametersValues());
        if(printInfo.getJasperPrint().getPages().isEmpty()){
            addWarningMessage(Messages.get("empty.report"));
            final ReportPrintInfo currentPrintInfo=getCurrentReportPrintInfo();
            form.setCurrentPage(currentPrintInfo==null ? 0 : currentPrintInfo.getPaginaAtual());
        } else {
            form.setCurrentPage(null);
            setCurrentReportPrintInfo(printInfo);
        }
    }

    /**
     * @see com.wim.reportviewer.web.report.list.ListReportsController#saveSearch(java.lang.String report)
     */
    @Override
    public void saveSearch(SaveSearchForm form)
    {
        if(StringUtils.isBlank(form.getReport())){
            WebAppUtil.getPageFlowScope().remove(SAVED_SEARCH);
        } else {
            WebAppUtil.getPageFlowScope().put(SAVED_SEARCH, form.getReport());
        }
    }

    /**
     * @see com.wim.reportviewer.web.report.list.ListReportsController#exportReport(char exportType)
     */
    @Override
    public void exportReport()
    {
        final ReportPrintInfo printInfo=getCurrentReportPrintInfo();
        if(printInfo != null){
            ReportUtil.exportReport(
                    printInfo, 
                    getResponse());
        }
    }
    
    @Override
    public String viewingReportExport(){
        super.viewingReportExport();
        return null;
    }

    /**
     * @see com.wim.reportviewer.web.report.list.ListReportsController#preparePage(int pages, int currentPage, java.lang.String pageReportContent)
     */
    @Override
    public void preparePage(PreparePageForm form)
    {
        final ReportPrintInfo printInfo=getCurrentReportPrintInfo();

        if(printInfo==null || printInfo.getJasperPrint().getPages().isEmpty()){
            form.setPageReportContent(null);
        } else {
            form.setPages(printInfo.getJasperPrint().getPages().size());

            if(form.getCurrentPage() == null || form.getCurrentPage() <= 0){
                form.setCurrentPage(1);
            } else if(form.getCurrentPage() > form.getPages()){
                form.setCurrentPage(form.getPages());
            }

            try {
                form.setPageReportContent(ReportUtil.generateHtmlBodyReportPage(
                        printInfo,
                        form.getCurrentPage()
                ));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            WebAppUtil.getPageFlowScope().put(ReportConstants.IMAGES_MAP, printInfo.getImagesMap());
        }
    }
}