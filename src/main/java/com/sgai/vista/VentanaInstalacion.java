package com.sgai.vista;

import com.sgai.controlador.ControladorAcademico;
import com.sgai.util.ExcepcionNegocio;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

// Aprovisionamiento separado: exige clave de instalación y una BD sin usuarios.
public final class VentanaInstalacion extends VentanaBase {

  private static final long serialVersionUID = 1L;
  private final JPasswordField autorizacion = Formulario.clave();
  private final JTextField usuario = Formulario.texto("", 40);
  private final JPasswordField primera = Formulario.clave();
  private final JPasswordField segunda = Formulario.clave();

  public VentanaInstalacion(ControladorAcademico controlador) {
    super(controlador, "SGAI · Instalación administrativa");
    JPanel contenido = Estilo.panel();
    contenido.add(
      Estilo.titulo("Instalación · Solo personal autorizado"),
      BorderLayout.NORTH
    );
    Formulario formulario = new Formulario();
    formulario.campo("Clave de instalación", autorizacion);
    formulario.campo("Usuario administrador", usuario);
    formulario.campo("Contraseña (12–128)", primera);
    formulario.campo("Confirmar contraseña", segunda);
    contenido.add(formulario, BorderLayout.CENTER);
    JPanel pie = new JPanel(new GridLayout(0, 1, 0, 8));
    pie.add(
      new JLabel(
        "Sin usuarios previos y con clave configurada en la estación administrada."
      )
    );
    JButton crear = Estilo.boton("Crear primer administrador", () ->
      accion(this::crear)
    );
    pie.add(crear);
    pie.add(estado);
    contenido.add(pie, BorderLayout.SOUTH);
    setContentPane(contenido);
    getRootPane().setDefaultButton(crear);
    pack();
    setMinimumSize(new Dimension(640, 390));
    setLocationRelativeTo(null);
  }

  // Autoriza antes de acceder a la BD; la confirmación transaccional sigue en el controlador.
  private void crear() {
    char[] llave = autorizacion.getPassword(),
      clave = primera.getPassword(),
      confirmacion = segunda.getPassword();
    autorizacion.setText("");
    primera.setText("");
    segunda.setText("");
    try {
      if (!Arrays.equals(clave, confirmacion)) throw new ExcepcionNegocio(
        "Las contraseñas no coinciden."
      );
      if (clave.length < 12 || clave.length > 128) throw new ExcepcionNegocio(
        "La contraseña requiere de 12 a 128 caracteres."
      );
      String nombre = usuario.getText().strip();
      // configurar clona la contraseña antes de que whenComplete borre este arreglo.
      var futuro = controlador
        .autorizarInstalacion(llave)
        .thenCompose(vacio -> {
          if (!vacio) throw new ExcepcionNegocio(
            "El sistema ya está configurado. Utilice el acceso habitual."
          );
          return controlador.configurar(nombre, clave);
        })
        .whenComplete((resultado, error) -> Arrays.fill(clave, '\0'));
      ejecutar(futuro, resultado -> {
        JOptionPane.showMessageDialog(
          this,
          "Administrador creado. Cierre el instalador e inicie SGAI mediante Aplicacion."
        );
        controlador.close();
        dispose();
      });
    } catch (RuntimeException error) {
      Arrays.fill(clave, '\0');
      throw error;
    } finally {
      Arrays.fill(llave, '\0');
      Arrays.fill(confirmacion, '\0');
    }
  }
}
