package com.vera5.httpd;

public class Config {

	public int port;
	public String DocumentRoot;
	public String footer;

	public String get(String value, String dflt) throws Exception {
		return (value.equals("") ? dflt : value);
	}

	// Overloaded
	public int get(String value, int dflt) throws Exception {
		return (value.equals("") ? dflt : Integer.parseInt(value));
	}
}
