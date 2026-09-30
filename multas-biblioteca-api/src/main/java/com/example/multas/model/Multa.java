package com.example.multas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "multas")
public class Multa {

    private static final BigDecimal VALOR_POR_DIA = new BigDecimal("500");
    private static final BigDecimal TOPE_MAXIMO = new BigDecimal("15000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código de estudiante es obligatorio")
    @Column(nullable = false)
    private String estudianteId;

    @NotBlank(message = "El concepto es obligatorio")
    private String concepto;

    @Min(value = 1, message = "Los días de atraso deben ser al menos 1")
    private int diasAtraso;

    @Column(nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    private EstadoMulta estado = EstadoMulta.PENDIENTE;

    private LocalDate fechaGeneracion = LocalDate.now();
    private LocalDate fechaPago;
    private String metodoPago;

    // Regla de negocio pura: no consulta el Repository ni ningún bean de
    // Spring, solo usa el dato que recibe como parámetro.
    public static BigDecimal calcularMonto(int diasAtraso) {
        BigDecimal calculado = VALOR_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso));
        return calculado.min(TOPE_MAXIMO);
    }

    public void marcarComoPagada(String metodoPago) {
        if (this.estado == EstadoMulta.PAGADA) {
            throw new MultaYaPagadaException(
                "La multa " + this.id + " ya fue pagada el " + this.fechaPago);
        }
        this.estado = EstadoMulta.PAGADA;
        this.fechaPago = LocalDate.now();
        this.metodoPago = metodoPago;
    }

    public Long getId() { return id; }

    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public int getDiasAtraso() { return diasAtraso; }
    public void setDiasAtraso(int diasAtraso) { this.diasAtraso = diasAtraso; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public EstadoMulta getEstado() { return estado; }
    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getMetodoPago() { return metodoPago; }
}