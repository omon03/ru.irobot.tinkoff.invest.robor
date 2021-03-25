import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.math.BigDecimal;

public class ParametersWindow extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextField ssoField;
    private JTextField profitField;
    private JTextField volumeField;
    private JCheckBox isSandBoxCheckBox;
    private JTextField comitionField;
    private JButton saveButton;
    private JTextField tickerField;
    private JCheckBox silentModeCheckBox;

    public ParametersWindow(TradingParameters parameters) {
        if(!(parameters.profit==null)) {profitField.setText(parameters.profit.toString());}
        if(!(parameters.value==null)) {volumeField.setText(parameters.value.toString());}
        if(!(parameters.commission==null)) {comitionField.setText(parameters.commission.toString());}
        if(!(parameters.ticker ==null)){tickerField.setText(parameters.ticker.toString());}
        isSandBoxCheckBox.setSelected(parameters.sandBox);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (ssoField.getText().length() > 2) {
                    parameters.ssoKey = ssoField.getText();
                    parameters.profit = new BigDecimal(profitField.getText());
                    parameters.value = new BigDecimal(volumeField.getText());
                    parameters.commission = new BigDecimal(comitionField.getText());
                    parameters.sandBox=isSandBoxCheckBox.isSelected();
                    parameters.ticker.set(0,tickerField.getText());
                    parameters.silentMode=silentModeCheckBox.isSelected();
                    try {
                        parameters.save();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dispose();
                } else { ssoField.setBackground(Color.red);}

            }
        });
    }

    public static void main(String[] args) {

    }
}
