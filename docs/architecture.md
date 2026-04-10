# Arquitetura do Sistema FPV Lupus

## Visão Geral

```
[ESP32-CAM] --HTTP MJPEG--> [App Android]
[ESP8266]   <--HTTP GET---  [App Android]
```

## Componentes

### ESP32-CAM (AI Thinker)
- Transmite stream de vídeo MJPEG via HTTP na porta 80
- Endpoint: `http://<IP>/stream`
- Resolução: HVGA (480x320), JPEG quality 12
- Clock: 20 MHz

### ESP8266 NodeMCU v3
- Controla dois servos (pan/tilt) via HTTP
- Endpoint: `http://<IP>/control?pan=<0-180>&tilt=<0-180>`
- Pan limitado: 40–140 graus
- Tilt limitado: 60–120 graus

### App Android
- Recebe stream MJPEG do ESP32-CAM
- Envia comandos HTTP ao ESP8266
- Exibe vídeo em modo VR (side-by-side)

## Rede

Todos os dispositivos conectados ao mesmo Wi-Fi.
Os IPs são atribuídos via DHCP — configure IPs fixos no roteador para evitar mudanças.

## Pinos ESP32-CAM (AI Thinker)

| Função | GPIO |
|--------|------|
| PWDN   | 32   |
| XCLK   | 0    |
| SIOD   | 26   |
| SIOC   | 27   |
| Y9–Y2  | 35,34,39,36,21,19,18,5 |
| VSYNC  | 25   |
| HREF   | 23   |
| PCLK   | 22   |

## Pinos ESP8266

| Função    | Pino |
|-----------|------|
| Servo Pan  | D5   |
| Servo Tilt | D6   |
