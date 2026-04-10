# Lista de Material e Ligações - FPV Lupus

## Lista de Material

### Eletrônica principal
- 1x ESP32-CAM (AI Thinker)
- 1x ESP8266 NodeMCU v3
- 2x Servo motor 9g (SG90)

### Alimentação
- 1x Fonte 5V 2A ou 3A (recomendado 3A)
- Jumpers (macho-macho e macho-fêmea)

### Opcional (recomendado)
- Protoboard
- Suporte pan/tilt para câmera (impressão 3D)
- Power bank (para mobilidade)

---

## Visão Geral do Sistema

```
ESP32-CAM  → stream de vídeo
ESP8266    → controle de servos
Fonte 5V   → alimenta tudo
```

---

## Ligação Elétrica

### Fonte 5V (alimentação central)

```
+5V → ESP32-CAM (pino 5V)
    → NodeMCU (VIN)
    → Servos (fio vermelho)

GND → ESP32-CAM (GND)
    → NodeMCU (GND)
    → Servos (fio marrom)
```

> Todos os GND precisam estar conectados juntos (GND comum).

---

### ESP8266 → Servos

**Servo PAN (esquerda/direita)**

| Fio do servo | Conectar em |
|---|---|
| Laranja (sinal) | D5 |
| Vermelho | 5V da fonte |
| Marrom | GND |

**Servo TILT (cima/baixo)**

| Fio do servo | Conectar em |
|---|---|
| Laranja (sinal) | D6 |
| Vermelho | 5V da fonte |
| Marrom | GND |

---

### ESP32-CAM

| Pino | Conectar em |
|---|---|
| 5V | Fonte 5V |
| GND | GND comum |

Sem servo conectado diretamente nela.

---

## Esquema Simplificado

```
        [FONTE 5V 2A+]
               |
      +--------+--------+
      |         |        |
  ESP32-CAM  NodeMCU  Servos
   (5V)       (VIN)    (VCC)
      |         |        |
      +--------+--------+
               |
           GND (comum)
```

---

## Erros Comuns

| Problema | Causa |
|---|---|
| Servo tremendo | Fonte fraca |
| ESP reiniciando | Alimentação incorreta |
| Sem resposta | GND não conectado |
| Imagem travando | Falta de energia |

**Nunca faca:**
- Alimentar servo no 3.3V
- Deixar GND desconectado entre placas
- Alimentar tudo somente pelo USB
- Usar fonte menor que 1A

---

## Dica

Use fonte 5V 3A. O Wi-Fi, a camera e os servos puxam pico de corrente ao mesmo tempo.
