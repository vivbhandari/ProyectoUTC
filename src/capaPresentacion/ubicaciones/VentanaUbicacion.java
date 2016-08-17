package capaPresentacion.ubicaciones;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import utilidades.Logging;
import utilidades.RequestFailureException;
import capaAPICliente.UbicacionCliente;
import capaNegocio.Ubicacion;
import capaNegocio.Usuario;
import capaPresentacion.VentanaOpciones;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class VentanaUbicacion extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField descripcionTxt;

	private VentanaOpciones ventanaOpciones;
	private boolean modoActualizar = false;
	private Ubicacion ubicacionAnterior;
	private Usuario usuarioActual = null;
	private String serverIP;
	Logger logger = Logging.obtenerClientLogger();

	public VentanaUbicacion(VentanaOpciones ventanaOpciones,
			Usuario usuarioActual, Ubicacion ubicacionAnterior,
			boolean modoActualizar, String serverIP) {
		this.ventanaOpciones = ventanaOpciones;
		this.usuarioActual = usuarioActual;
		this.ubicacionAnterior = ubicacionAnterior;
		this.modoActualizar = modoActualizar;
		this.serverIP = serverIP;
		init();

		if (modoActualizar) {
			setTitle("Actualizar Ubicaci�n");
			crearGui(ubicacionAnterior);
		} else {
			setTitle("Agregar Ubicaci�n");
		}
	}

	public VentanaUbicacion(VentanaOpciones ventanaOpciones,
			Usuario usuarioActual, String serverIP) {
		this(ventanaOpciones, usuarioActual, null, false, serverIP);
	}

	private void init() {

		setBounds(100, 100, 250, 150);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout((LayoutManager) new FormLayout(
				new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, }));

		JLabel lblNewLabel = new JLabel("Descripci�n");
		lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		contentPanel.add(lblNewLabel, "2, 2, right, default");
		descripcionTxt = new JTextField();
		contentPanel.add(descripcionTxt, "4, 2, fill, default");
		descripcionTxt.setColumns(10);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					guardarUbicacion();
				}
			});
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}
		{
			JButton cancelButton = new JButton("Cancelar");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			cancelButton.setActionCommand("Cancelar");
			buttonPane.add(cancelButton);
		}
	}

	private void crearGui(Ubicacion ubicacion) {
		descripcionTxt.setText(ubicacion.getDescripcion());
	}

	private void guardarUbicacion() {

		String descripcion = descripcionTxt.getText();
		Ubicacion ubicacion = null;

		if (descripcion.isEmpty()) {

			JOptionPane.showMessageDialog(VentanaUbicacion.this,
					"Debes llenar toda la informaci�n, intenta de nuevo",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		} else {
			TablaUbicacionesModelo tablaUbicaciones = ventanaOpciones
					.getTablaUbicacionesModelo();
			for (Ubicacion ubicacionExistente : tablaUbicaciones
					.getUbicaciones()) {
				if (ubicacionExistente.getDescripcion().equals(descripcion)
						&& (!modoActualizar || ubicacionExistente
								.getIdUbicacion() != ubicacionAnterior
								.getIdUbicacion())) {
					JOptionPane.showMessageDialog(ventanaOpciones,
							"Ubicaci�n ya existe", "Creaci�n Fallida",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			if (modoActualizar) {
				ubicacion = ubicacionAnterior;
				ubicacion.setDescripcion(descripcion);
			} else {
				ubicacion = new Ubicacion(descripcion);
			}
		}

		try {
			// guardar en la BD
			UbicacionCliente ubicacionCliente = new UbicacionCliente(
					usuarioActual.getNombreUsuario(), usuarioActual.getClave(),
					serverIP);

			if (modoActualizar) {

				// revisar si es necesario actualizar los campos del GUI para la
				// ubicacion actual
				if (ventanaOpciones.obtenerIdUbicacionActual() == ubicacion
						.getIdUbicacion()) {
					logger.log(Level.INFO, "Ubicaci�n ya existe");
				} else {
					ubicacionCliente.actualizar(ubicacion);
				}
			} else {
				ubicacionCliente.agregar(ubicacion);
			}
			// cerrar ventana
			setVisible(false);

			// refrescar lista GUI
			ventanaOpciones.refrescarVistaUbicaciones(true);

			// mostrar mensaje exitoso
			JOptionPane.showMessageDialog(ventanaOpciones,
					"Ubicaci�n guardada exitosamente.", "Ubicaci�n guardada",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (RequestFailureException exc) {
			logger.log(Level.SEVERE, "Error guardando ubicaci�n", exc);
			JOptionPane.showMessageDialog(ventanaOpciones,
					"Error al guardar ubicaci�n: " + exc.getMessage(),
					"Error: " + exc.getHttpCode(), JOptionPane.ERROR_MESSAGE);
		}
	}
}
