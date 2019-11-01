package com.avob.openadr.server.oadr20b.vtn.exception.eireport;

import com.avob.openadr.model.oadr20b.exception.Oadr20bException;
import com.avob.openadr.model.oadr20b.oadr.OadrResponseType;
import com.avob.openadr.server.oadr20b.vtn.exception.Oadr20bGenericException;

public class Oadr20bCreatedReportApplicationLayerException extends Oadr20bException implements Oadr20bGenericException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1468896185130868972L;

	private final transient OadrResponseType response;
	private final transient boolean signed;

	public Oadr20bCreatedReportApplicationLayerException(String message, OadrResponseType response, boolean signed) {
		super(message);
		this.response = response;
		this.signed = signed;
	}

	@Override
	public OadrResponseType getResponse() {
		return response;
	}

	@Override
	public boolean isSigned() {
		return signed;
	}
}
