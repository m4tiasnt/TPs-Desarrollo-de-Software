import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {
    public static void main(String[] args) {
        // Inicializa la unidad de persistencia que creamos en el XML
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        
        System.out.println("¡Conexión exitosa y tablas creadas en PostgreSQL!");
        
        emf.close();
    }
}