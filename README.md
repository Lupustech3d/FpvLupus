# FPV Lupus

Carrinho FPV controlado por Wi-Fi com stream de vídeo ao vivo e controle de câmera pan/tilt.

| Componente | Status | Funcao |
|---|---|---|
| ESP32-CAM firmware | Pronto | Transmite vídeo MJPEG via HTTP |
| ESP8266 firmware | Pronto | Controla servos pan/tilt via HTTP |
| App Android | Em desenvolvimento | Exibe vídeo em modo VR (controle de servo ainda nao integrado) |

---

## Estado atual

O app Android atualmente **exibe o stream de vídeo** da ESP32-CAM em modo VR (side-by-side).

O controle dos servos via ESP8266 **ainda nao esta integrado ao app** — por enquanto pode ser testado direto pelo navegador.

```
PRONTO:
  ESP32-CAM  ──── MJPEG/HTTP ────> App Android (vídeo ok)
  ESP8266    ──── HTTP GET   <───  Navegador (teste manual)

PROXIMO PASSO:
  ESP8266    <─── HTTP GET ──────  App Android (a implementar)
```

Todos os dispositivos precisam estar no mesmo Wi-Fi.

---

## Firmware

### ESP32-CAM — stream de vídeo

Arquivo: `esp32-cam/camera_stream.ino`

Acesse o stream pelo navegador ou pelo app:

```
http://IP_DA_ESP32/stream
```

### ESP8266 — controle de servos

Arquivo: `esp8266-servo/servo_control.ino`

Envie comandos via HTTP GET:

```
http://IP_DO_NODEMCU/control?pan=90&tilt=90
```

| Parâmetro | Faixa | Descrição |
|-----------|-------|-----------|
| pan       | 40–140 | Esquerda/direita |
| tilt      | 60–120 | Cima/baixo |

> Antes de gravar, edite `ssid` e `password` no topo de cada arquivo `.ino` com suas credenciais Wi-Fi.

---

## Lista de Material

| Qtd | Componente |
|-----|-----------|
| 1x | ESP32-CAM (AI Thinker) |
| 1x | ESP8266 NodeMCU v3 |
| 2x | Servo motor 9g (SG90) |
| 1x | Fonte 5V 2A (mínimo) — recomendado 3A |
| — | Jumpers macho-macho e macho-fêmea |
| — | Protoboard (opcional, facilita montagem) |
| — | Power bank 5V (para mobilidade) |
| — | Suporte pan/tilt (impressão 3D) |

---

## Ligações Elétricas

### Alimentação (GND comum obrigatório)

```
Fonte 5V (+) ──> ESP32-CAM (5V)
             ──> NodeMCU (VIN)
             ──> Servos (fio vermelho)

Fonte 5V (−) ──> ESP32-CAM (GND)
             ──> NodeMCU (GND)
             ──> Servos (fio marrom)
```

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

### ESP8266 → Servos

**Servo PAN (esquerda/direita)**

| Fio | Conectar |
|-----|----------|
| Laranja (sinal) | D5 |
| Vermelho | +5V da fonte |
| Marrom | GND |

**Servo TILT (cima/baixo)**

| Fio | Conectar |
|-----|----------|
| Laranja (sinal) | D6 |
| Vermelho | +5V da fonte |
| Marrom | GND |

### Pinos ESP32-CAM (AI Thinker)

| Sinal | GPIO |
|-------|------|
| PWDN | 32 |
| XCLK | 0 |
| SIOD | 26 |
| SIOC | 27 |
| Y9–Y2 | 35, 34, 39, 36, 21, 19, 18, 5 |
| VSYNC | 25 |
| HREF | 23 |
| PCLK | 22 |

---

## Erros Comuns

| Sintoma | Causa |
|---------|-------|
| Servo tremendo | Fonte fraca |
| ESP reiniciando | Alimentação insuficiente |
| Sem resposta | GND não conectado entre placas |
| Imagem travando | Falta de corrente |

**Nunca faca:**
- Alimentar servo no pino 3.3V
- Alimentar tudo somente pelo cabo USB
- Usar fonte abaixo de 1A
- Deixar GND desconectado entre as placas

---

## Proximos Passos

- [ ] Integrar controle de servos no app Android (HTTP para ESP8266)
- [ ] Modo VR completo com controle por movimento da cabeca (giroscopio)
- [ ] Suavizacao de movimento dos servos
- [ ] Reducao de latencia do stream

---

## Documentacao

- [Ligacoes eletricas detalhadas](docs/wiring.md)
- [Arquitetura do sistema](docs/architecture.md)

---

## Autor

**LupusTech**
