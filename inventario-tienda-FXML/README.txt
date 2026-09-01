# INVENTARIO TIENDA - INSTALACION

1. Crea la BD: ejecuta database.sql en pgAdmin o psql
2. Edita dao/ConexionDB.java con tu usuario/pass de postgres
3. mvn javafx:run  (o Run Main.java desde IntelliJ con VM options: --module-path /path/javafx --add-modules javafx.controls,javafx.fxml)

FUNCIONES:
- Tabla editable: doble click en talla para cambiar cantidad
- Paginacion: 8 productos por pagina
- Buscador: filtra en tiempo real por marca/referencia/nombre y muestra fila completa con tallas, total y foto
- Fotos: selecciona fila + boton foto
- Marcas: crea marcas nuevas

FOTO: se guardan en carpeta /fotos/
