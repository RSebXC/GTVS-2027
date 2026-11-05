"""Red abstracta: los nodos no son coordenadas ni calles de Guatemala."""
from datetime import timedelta

VERSION_RED = 'red-sintetica-v1'
MODELO = {'version': 'demostracion-v1', 'semivida_dias': 7, 'referencia_tiempo_s': 60,
          'referencia_distancia_m': 1000, 'referencia_costo_riesgo': 1,
          'advertencia': 'Parámetros ilustrativos, sin calibración ni validez predictiva.'}
NODOS = [{'id': chr(65+i), 'nombre': f'Punto {chr(65+i)}', 'x': 100+(i%3)*260,
          'y': 90+(i//3)*170} for i in range(9)]
# id, origen, destino, metros, segundos, cobertura sintética declarada.
TRAMOS = [
    ('AB','A','B',200,20,True),('BC','B','C',200,20,True),
    ('CF','C','F',200,20,True),('FI','F','I',200,20,True),
    ('AD','A','D',260,30,True),('DE','D','E',260,30,True),
    ('EF','E','F',260,30,True),('DG','D','G',330,45,True),
    ('GH','G','H',330,45,True),('HI','H','I',330,45,True),
    ('BE','B','E',220,25,True),('EH','E','H',220,25,False),
]
PERFILES = {
    'rapida': {'nombre':'Rápida','distancia':0.0,'tiempo':1.0,'riesgo':0.0},
    'equilibrada': {'nombre':'Equilibrada','distancia':0.15,'tiempo':1.0,'riesgo':1.5},
    'menor_riesgo_estimado': {'nombre':'Menor riesgo estimado','distancia':0.0,'tiempo':0.1,'riesgo':10.0},
}

def semilla(now):
    return [('inc-demo-1','BC',4,(now-timedelta(days=1)).isoformat(),0.8),
            ('inc-demo-2','BC',3,(now-timedelta(days=2)).isoformat(),0.7),
            ('inc-demo-3','EF',2,(now-timedelta(days=1)).isoformat(),0.8)]
