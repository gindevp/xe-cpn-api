package com.mycompany.myapp.service.invoice;

public class MeInvoiceException extends RuntimeException {

    public MeInvoiceException(String message) {
        super(message);
    }

    public MeInvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
