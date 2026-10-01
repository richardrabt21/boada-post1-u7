import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Simulador de las pasarelas de pago para probar la Parte 2.
// PagosUDES en el puerto 9001 y Wompi en el 9002.
// Regla: se aprueban montos hasta 10000; los mayores se rechazan.
public class PasarelasSimuladas {

    private static final double LIMITE_MONTO = 10000;
    private static final AtomicInteger contador = new AtomicInteger(1000);

    public static void main(String[] args) throws IOException {
        HttpServer pagosUdes = HttpServer.create(new InetSocketAddress(9001), 0);
        pagosUdes.createContext("/pagosudes/transacciones", PasarelasSimuladas::pagosUdes);
        pagosUdes.start();

        HttpServer wompi = HttpServer.create(new InetSocketAddress(9002), 0);
        wompi.createContext("/wompi/transactions", PasarelasSimuladas::wompi);
        wompi.start();

        System.out.println("PagosUDES simulada: http://localhost:9001/pagosudes/transacciones");
        System.out.println("Wompi simulada:     http://localhost:9002/wompi/transactions");
        System.out.println("Regla: se aprueban montos hasta 10000, los mayores se rechazan.");
        System.out.println("Ctrl + C para detener.");
    }

    private static void pagosUdes(HttpExchange ex) throws IOException {
        String body = leer(ex);
        double monto = numero(body, "\"monto\"\\s*:\\s*([0-9.]+)");
        boolean aprobado = monto <= LIMITE_MONTO;
        String estado = aprobado ? "APROBADA" : "RECHAZADA";
        String json = "{\"idTransaccion\":\"PU-" + contador.incrementAndGet()
            + "\",\"estadoTransaccion\":\"" + estado + "\"}";
        System.out.println("[PAGOSUDES] recibido " + body + " -> " + estado);
        responder(ex, json);
    }

    private static void wompi(HttpExchange ex) throws IOException {
        String body = leer(ex);
        double centavos = numero(body, "\"amountInCents\"\\s*:\\s*([0-9]+)");
        boolean aprobado = (centavos / 100) <= LIMITE_MONTO;
        String estado = aprobado ? "APPROVED" : "DECLINED";
        String referencia = texto(body, "\"reference\"\\s*:\\s*\"([^\"]*)\"");
        String json = "{\"reference\":\"" + referencia + "\",\"status\":\"" + estado + "\"}";
        System.out.println("[WOMPI] recibido " + body + " -> " + estado);
        responder(ex, json);
    }

    private static String leer(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static double numero(String texto, String patron) {
        Matcher m = Pattern.compile(patron).matcher(texto);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    private static String texto(String texto, String patron) {
        Matcher m = Pattern.compile(patron).matcher(texto);
        return m.find() ? m.group(1) : "";
    }

    private static void responder(HttpExchange ex, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}