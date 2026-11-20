package com.sportapp.tiendasport.service;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.model.EscalaDescuento;
import com.sportapp.tiendasport.repository.EscalaDescuentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DescuentoService {

    static final int MAX_ESCALAS = 10;
    static final BigDecimal MAX_PORCENTAJE = new BigDecimal("90");

    private final EscalaDescuentoRepository repositorio;
    private final ActividadService actividadService;

    public List<EscalaDescuento> listar() {
        return repositorio.findAllByOrderByMontoMinimoAsc();
    }

    /**
     * Reemplaza TODA la escala por la nueva. Reglas: máximo 10 escalones, montos mayores a 0 y no
     * repetidos, porcentaje entre 0 (sin incluir) y 90, y a mayor monto, mayor porcentaje
     * (no tendría sentido darle menos descuento a quien compra más).
     */
    @Transactional
    public List<EscalaDescuento> reemplazar(List<EscalaDescuento> nuevas, Cliente actor) {
        if (nuevas == null) nuevas = List.of();
        if (nuevas.size() > MAX_ESCALAS) {
            throw new IllegalArgumentException("Máximo " + MAX_ESCALAS + " escalones de descuento.");
        }
        List<EscalaDescuento> ordenadas = new ArrayList<>(nuevas);
        for (EscalaDescuento e : ordenadas) {
            if (e.getMontoMinimo() == null || e.getMontoMinimo().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Cada escalón necesita un monto mayor a 0.");
            }
            if (e.getPorcentaje() == null || e.getPorcentaje().compareTo(BigDecimal.ZERO) <= 0
                    || e.getPorcentaje().compareTo(MAX_PORCENTAJE) > 0) {
                throw new IllegalArgumentException("El porcentaje debe ser mayor a 0 y máximo " + MAX_PORCENTAJE + ".");
            }
        }
        ordenadas.sort(Comparator.comparing(EscalaDescuento::getMontoMinimo));
        for (int i = 1; i < ordenadas.size(); i++) {
            EscalaDescuento anterior = ordenadas.get(i - 1);
            EscalaDescuento actual = ordenadas.get(i);
            if (anterior.getMontoMinimo().compareTo(actual.getMontoMinimo()) == 0) {
                throw new IllegalArgumentException("Hay dos escalones con el mismo monto.");
            }
            if (actual.getPorcentaje().compareTo(anterior.getPorcentaje()) <= 0) {
                throw new IllegalArgumentException("A mayor monto de compra, el porcentaje de descuento debe ser mayor.");
            }
        }

        String antes = describir(repositorio.findAllByOrderByMontoMinimoAsc());
        // deleteAllInBatch borra YA (en la base de datos). Con deleteAll() Hibernate hace primero los INSERT y
        // después los DELETE, y el monto repetido chocaba con el índice único (error "ya existe un registro").
        repositorio.deleteAllInBatch();
        repositorio.flush();
        List<EscalaDescuento> limpias = new ArrayList<>();
        for (EscalaDescuento e : ordenadas) {
            limpias.add(new EscalaDescuento(null, e.getMontoMinimo(), e.getPorcentaje()));
        }
        List<EscalaDescuento> guardadas = repositorio.saveAll(limpias);
        actividadService.registrar(actor, "DESCUENTOS_ACTUALIZADOS", "descuento", null,
                "Antes: " + antes + ". Ahora: " + describir(ordenadas));
        return guardadas;
    }

    private static String describir(List<EscalaDescuento> escalas) {
        if (escalas.isEmpty()) return "sin descuentos";
        StringBuilder sb = new StringBuilder();
        for (EscalaDescuento e : escalas) {
            sb.append("desde $").append(e.getMontoMinimo().toPlainString()).append(" = ")
              .append(e.getPorcentaje().stripTrailingZeros().toPlainString()).append("%; ");
        }
        return sb.toString().trim();
    }
}
