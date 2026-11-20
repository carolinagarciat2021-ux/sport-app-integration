package com.sportapp.tiendasport.repository;

import com.sportapp.tiendasport.model.Pedido;
import com.sportapp.tiendasport.dto.PedidoListado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByIdCliente(Integer idCliente);

    List<Pedido> findByIdClienteOrderByFechaDescIdPedidoDesc(Integer idCliente);

    /**
     * Listado para Administrador y Vendedor: cada pedido con el nombre y correo de quien compró (JOIN con clientes),
     * filtrado por fecha, valor, estado y texto (nombre, apellido, correo o identificación), más recientes primero.
     * Ningún parámetro es null (la capa de servicio pone valores por defecto), así se evitan problemas de tipos.
     */
    @Query(value = "select new com.sportapp.tiendasport.dto.PedidoListado(p.idPedido, p.fecha, p.estado, p.subtotal, "
            + "p.descuentoPorcentaje, p.descuentoValor, p.total, p.direccionEntrega, c.idClientes, c.nombre, c.apellido, c.correo, c.telefono, p.transportadora, p.numeroGuia) "
            + "from Pedido p, Cliente c where p.idCliente = c.idClientes "
            + "and p.fecha >= :desde and p.fecha < :hasta and p.total >= :min and p.total <= :max "
            + "and (:estado = '' or p.estado = :estado) "
            + "and (:texto = '' or lower(c.nombre) like :patron or lower(c.apellido) like :patron or lower(c.correo) like :patron "
            + "or lower(concat(c.nombre, ' ', c.apellido)) like :patron or c.identificacion like :patron) "
            + "order by p.fecha desc, p.idPedido desc",
           countQuery = "select count(p) from Pedido p, Cliente c where p.idCliente = c.idClientes "
            + "and p.fecha >= :desde and p.fecha < :hasta and p.total >= :min and p.total <= :max "
            + "and (:estado = '' or p.estado = :estado) "
            + "and (:texto = '' or lower(c.nombre) like :patron or lower(c.apellido) like :patron or lower(c.correo) like :patron "
            + "or lower(concat(c.nombre, ' ', c.apellido)) like :patron or c.identificacion like :patron)")
    Page<PedidoListado> buscarListado(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                      @Param("min") BigDecimal min, @Param("max") BigDecimal max,
                                      @Param("estado") String estado, @Param("texto") String texto,
                                      @Param("patron") String patron, Pageable pageable);
}
