package com.wim.reportviewer.web.report;

import java.util.Map;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JasperDesign;

public class ReportPrintInfo {
    private JasperPrint jasperPrint;
    private JasperDesign jasperDesign;
    private Map<String, Object> valoresParametros;
    private Integer paginaAtual;
    private Map<String,Object> imagesMap;
    public ReportPrintInfo(JasperPrint jasperPrint, JasperDesign jasperDesign,
            Map<String, Object> valoresParametros) {
        super();
        this.jasperPrint = jasperPrint;
        this.jasperDesign = jasperDesign;
        this.valoresParametros = valoresParametros;
        this.paginaAtual=null;
        this.imagesMap=null;
    }
    public JasperPrint getJasperPrint() {
        return jasperPrint;
    }
    public JasperDesign getJasperDesign() {
        return jasperDesign;
    }
    public Map<String, Object> getValoresParametros() {
        return valoresParametros;
    }
    public Map<String, Object> getImagesMap() {
        return imagesMap;
    }
    public void setImagesMap(Map<String, Object> imagesMap) {
        this.imagesMap = imagesMap;
    }
    public Integer getPaginaAtual() {
        return paginaAtual;
    }
    public void setPaginaAtual(Integer paginaAtual) {
        this.paginaAtual = paginaAtual;
    }
}
