package cl.duoc.bancoxyz.backend.dto;

public class RetiroRealizadoEvent {

    private Long cuentaId;
    private Double monto;
    private Double saldoFinal;
    private String tipo;

    public RetiroRealizadoEvent() {
    }

    public RetiroRealizadoEvent(Long cuentaId, Double monto, Double saldoFinal, String tipo) {
        this.cuentaId = cuentaId;
        this.monto = monto;
        this.saldoFinal = saldoFinal;
        this.tipo = tipo;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Double getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(Double saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}