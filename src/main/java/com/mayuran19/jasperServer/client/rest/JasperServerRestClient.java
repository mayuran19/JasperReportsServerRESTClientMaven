package com.mayuran19.jasperServer.client.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;

import com.mayuran19.jasperServer.client.runReport.request.ObjectFactory;
import com.mayuran19.jasperServer.client.runReport.request.ResourceDescriptor;
import com.mayuran19.jasperServer.client.runReport.response.Report;

public class JasperServerRestClient {
	private static final Logger LOGGER = Logger
			.getLogger(JasperServerRestClient.class);
	private static final String JASPER_SERVER_LOGIN_FORM_USERNAME_NAME = "j_username";
	private static final String JASPER_SERVER_LOGIN_FORM_PASSWORD_NAME = "j_password";
	Series<CookieSetting> sessionCookies = null;
	private ResourceBundle resourceBundle = ResourceBundle
			.getBundle("configuration");
	private String JASPER_SERVER_LOGIN_URL = "jasperServer.login.url";
	private String JASPER_SERVER_LOGIN_USERNAME = "jasperServer.login.username";
	private String JASPER_SERVER_LOGIN_PASSWORD = "jasperServer.login.password";
	private String JASPER_SERVER_HOME_URL = "jasperServer.home.url";

	public void loginToJasperServer() {
		Client client = new Client(Protocol.HTTP);
		Request request = new Request();
		request.setResourceRef(this.resourceBundle
				.getString(JASPER_SERVER_LOGIN_URL));
		request.setMethod(Method.POST);
		Form form = new Form();
		form.set(JASPER_SERVER_LOGIN_FORM_USERNAME_NAME,
				this.resourceBundle.getString(JASPER_SERVER_LOGIN_USERNAME));
		form.set(JASPER_SERVER_LOGIN_FORM_PASSWORD_NAME,
				this.resourceBundle.getString(JASPER_SERVER_LOGIN_PASSWORD));
		Representation representation = form.getWebRepresentation();
		request.setEntity(representation);
		Response response = client.handle(request);
		this.sessionCookies = response.getCookieSettings();
	}

	public InputStream downloadReport(String reportName, String outputFormat) {
		InputStream is = null;
		Report report = this.runReport(reportName, outputFormat);
		Client client = new Client(Protocol.HTTP);
		Request request = new Request();
		for (CookieSetting cookieSetting : sessionCookies) {
			request.getCookies().add(cookieSetting);
		}
		request.setResourceRef(this.getGeneratedReportDownloadURL(report));
		request.setMethod(Method.GET);
		Response response = client.handle(request);
		try {
			is = response.getEntity().getStream();
		} catch (IOException e) {
			LOGGER.error("Exception", e);
		}

		return is;
	}

	public Report runReport(String reportName, String outputFormat) {
		String reportURI = "/reports/" + reportName;
		Client client = new Client(Protocol.HTTP);
		Request request = new Request();
		if (this.sessionCookies == null) {
			this.loginToJasperServer();
		}
		for (CookieSetting cookieSetting : sessionCookies) {
			request.getCookies().add(cookieSetting);
		}
		request.setResourceRef(this.getReportURLByReportName(reportName,
				outputFormat));
		request.setMethod(Method.PUT);
		Representation representation = new StringRepresentation(
				this.getRunReportResourceDiscriptorXML(reportName, reportURI),
				MediaType.TEXT_XML);
		request.setEntity(representation);
		Response response = client.handle(request);
		Report report = this.getJasperReportRunResponse(response.getEntity());

		return report;
	}

	public String getReportURLByReportName(String reportName,
			String outputFormat) {
		String str = this.resourceBundle.getString(JASPER_SERVER_HOME_URL)
				+ "rest/report/" + reportName + "?RUN_OUTPUT_FORMAT="
				+ outputFormat;
		return str;
	}

	public String getRunReportResourceDiscriptorXML(String reportName,
			String reportURI) {
		String xml = null;
		ObjectFactory factory = new ObjectFactory();
		ResourceDescriptor resourceDescriptor = factory
				.createResourceDescriptor();
		resourceDescriptor.setIsNew("false");
		resourceDescriptor.setName(reportName);
		resourceDescriptor.setUriString(reportURI);
		resourceDescriptor.setWsType("reportUnit");
		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(ResourceDescriptor.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			StringWriter stringWriter = new StringWriter();
			marshaller.marshal(resourceDescriptor, stringWriter);
			xml = stringWriter.toString();
		} catch (JAXBException e) {
			LOGGER.error("Exception", e);
		}

		return xml;
	}

	public Report getJasperReportRunResponse(Representation representation) {
		Report report = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			report = (Report) unmarshaller
					.unmarshal(representation.getReader());
		} catch (IOException e) {
			LOGGER.error(
					"Exception occured while getting runReport resulting xml",
					e);
		} catch (JAXBException e) {
			LOGGER.error("Exception occured while getting jaxbContext", e);
		}
		return report;
	}

	public String getGeneratedReportDownloadURL(Report report) {
		String str = this.resourceBundle.getString(JASPER_SERVER_HOME_URL)
				+ "rest/report/" + report.getUuid() + "?file=report";
		return str;
	}
}
