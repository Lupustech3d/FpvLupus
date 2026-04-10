# FPV Lupus

Carrinho FPV controlado por Wi-Fi com stream de vídeo ao vivo, controle de câmera pan/tilt por giroscópio e visualização em modo VR.

| Componente | Status | Função |
|---|---|---|
| ESP32-CAM firmware | Pronto | Stream de vídeo MJPEG via HTTP |
| ESP8266 firmware | Pronto | Controla servos pan/tilt via HTTP |
| App Android | Pronto | VR + head tracking + controle de servo |

---

## Como funciona

```
[ESP32-CAM] ──── MJPEG/HTTP ────> App Android (exibe em VR)
[ESP8266]   <─── HTTP GET ──────  App Android (envia ângulos pan/tilt)
                                        ^
                               Giroscópio do celular
```

Todos os dispositivos conectados ao mesmo Wi-Fi.
O celular lê o giroscópio, calcula os ângulos e envia comandos ao ESP8266 em tempo real.

---

## App Android

Pasta: `android-app/`

**Funcionalidades:**
- Stream de vídeo MJPEG em modo VR (side-by-side)
- Head tracking por giroscópio — move os servos com a cabeça
- Controle de servo via HTTP GET ao ESP8266
- Tela de configurações (IP da câmera, IP do servo, sensibilidade, limites, suavização)
- Botão de recentrar (recalibra o giroscópio)
- Botão de centralizar servos
- Status em tempo real de câmera, servo e tracking

**Configurações disponíveis no app:**
| Parâmetro | Descrição |
|---|---|
| URL da câmera | Endereço HTTP do stream MJPEG |
| IP do servo | IP do ESP8266 |
| Sensibilidade H/V | Ganho do giroscópio por eixo |
| Inverter H/V | Inverte direção do movimento |
| Pan min/max | Limite de ângulo horizontal |
| Tilt min/max | Limite de ângulo vertical |
| Dead zone | Zona morta do giroscópio (graus) |
| Suavização | Filtro de movimento (0 = cru, 1 = suave) |
| Intervalo de envio | Frequência dos comandos HTTP (ms) |

---

## Firmware

### ESP32-CAM — stream de vídeo

Arquivo: `esp32-cam/camera_stream.ino`

```
http://IP_DA_ESP32/stream
```

### ESP8266 — controle de servos

Arquivo: `esp8266-servo/servo_control.ino`

```
http://IP_DO_NODEMCU/control?pan=90&tilt=90
```

| Parâmetro | Faixa | Descrição |
|---|---|---|
| pan | 40–140 | Esquerda/direita |
| tilt | 60–120 | Cima/baixo |

> Antes de gravar, edite `ssid` e `password` no topo de cada `.ino` com suas credenciais Wi-Fi.

---

## Lista de Material

| Qtd | Componente |
|---|---|
| 1x | ESP32-CAM (AI Thinker) |
| 1x | ESP8266 NodeMCU v3 |
| 2x | Servo motor 9g (SG90) |
| 1x | Fonte 5V 2A (mínimo) — recomendado 3A |
| — | Jumpers macho-macho e macho-fêmea |
| — | Protoboard (opcional) |
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
|---|---|
| Laranja (sinal) | D5 |
| Vermelho | +5V da fonte |
| Marrom | GND |

**Servo TILT (cima/baixo)**

| Fio | Conectar |
|---|---|
| Laranja (sinal) | D6 |
| Vermelho | +5V da fonte |
| Marrom | GND |

### Pinos ESP32-CAM (AI Thinker)

| Sinal | GPIO |
|---|---|
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
|---|---|
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

- [ ] Suavização adicional dos servos
- [ ] Reducao de latência do stream
- [ ] Controle de direção do carrinho pelo app
- [ ] Modo autônomo

---

## Documentação

- [Ligações elétricas detalhadas](docs/wiring.md)
- [Arquitetura do sistema](docs/architecture.md)

---

## Autor

**LupusTech**
