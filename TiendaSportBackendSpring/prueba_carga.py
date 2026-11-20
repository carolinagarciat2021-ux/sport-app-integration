#!/usr/bin/env python3
"""
Prueba de carga ligera de Tienda Sport (RNF-08, eficiencia de desempeño).

Simula varios usuarios al mismo tiempo consultando una ruta pública del API
y calcula: porcentaje de éxito, tiempo promedio, percentil 95 y tiempo máximo.
Solo usa la librería estándar de Python (no hay que instalar nada).

Uso (con el API encendido):
    python prueba_carga.py
    python prueba_carga.py http://localhost:8081/api/producto 50 20

Argumentos opcionales: URL, usuarios simultáneos, peticiones por usuario.
Criterios de aprobación definidos en el informe: éxito >= 99 % y p95 <= 2000 ms.
"""
import sys
import time
import statistics
import urllib.request
from concurrent.futures import ThreadPoolExecutor

URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081/api/categorias"
USUARIOS = int(sys.argv[2]) if len(sys.argv) > 2 else 10
PETICIONES = int(sys.argv[3]) if len(sys.argv) > 3 else 20


def un_usuario(_):
    resultados = []
    for _ in range(PETICIONES):
        inicio = time.perf_counter()
        try:
            with urllib.request.urlopen(URL, timeout=10) as r:
                r.read()
                ok = r.status == 200
        except Exception:
            ok = False
        resultados.append((ok, (time.perf_counter() - inicio) * 1000))
    return resultados


def main():
    print(f"Prueba de carga a {URL}")
    print(f"{USUARIOS} usuarios simultáneos x {PETICIONES} peticiones cada uno")
    inicio = time.perf_counter()
    with ThreadPoolExecutor(max_workers=USUARIOS) as pool:
        listas = list(pool.map(un_usuario, range(USUARIOS)))
    duracion = time.perf_counter() - inicio

    todos = [x for lista in listas for x in lista]
    tiempos = sorted(t for ok, t in todos if ok)
    total = len(todos)
    exitosas = len(tiempos)
    exito = exitosas / total * 100

    print("-" * 50)
    print(f"Peticiones totales : {total}")
    print(f"Peticiones exitosas: {exitosas} ({exito:.1f} %)")
    print(f"Duración total     : {duracion:.1f} s ({total / duracion:.1f} peticiones/s)")
    if tiempos:
        p95 = tiempos[min(len(tiempos) - 1, int(len(tiempos) * 0.95))]
        print(f"Tiempo promedio    : {statistics.mean(tiempos):.0f} ms")
        print(f"Percentil 95 (p95) : {p95:.0f} ms")
        print(f"Tiempo máximo      : {max(tiempos):.0f} ms")
        cumple = exito >= 99 and p95 <= 2000
    else:
        cumple = False
    print("-" * 50)
    print("RESULTADO:", "CUMPLE (éxito >= 99 % y p95 <= 2000 ms)" if cumple else "NO CUMPLE")


if __name__ == "__main__":
    main()
