import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.adapter.DSPortAdapter;

/**
 */
public class OneWireSetup
   extends JDialog
   implements ActionListener
{
   private static final Font fontBigBold = new Font("DialogInput", Font.BOLD, 14);
   private static final Font fontBigPlain = new Font("DialogInput", Font.PLAIN, 14);
   private static final Font fontBold = new Font("DialogInput", Font.BOLD, 12);
   private static final Font fontPlain = new Font("DialogInput", Font.PLAIN, 12);
   private static final Font fontSmallBold = new Font("DialogInput", Font.BOLD, 9);
   private static final Font fontSmallPlain = new Font("DialogInput", Font.PLAIN, 9);

   public static final int MAX_INDEX = 2;
   public static final int MIN_INDEX = 0;

   JFrame window;
   JButton prev, next, cancel, help;
   int currentIndex = MIN_INDEX;
   AdapterChooser ac = null;
   JPanel pollRatePanel = null;
   boolean isCanceled = false;
   JCheckBoxMenuItem[] pollRateButtons = null;
   JPanel searchModePanel = null;
   JCheckBox normalModeEnabled = null, taggingModeEnabled = null;
   Container contentPane = null;
   Component comp = null;


   public OneWireSetup(JFrame parent, boolean modal)
   {
      super(parent, "1-Wire API for Java Setup Wizard", modal);

      // setup the contents
      contentPane = getContentPane();
      contentPane.setLayout(new BorderLayout(5,2));
      contentPane.add(getSideBarPanel(), BorderLayout.WEST);
      contentPane.add(getButtonPanel(), BorderLayout.SOUTH);
      setCenterPanel();
      pack();
      getRootPane().setPreferredSize(new Dimension(640,400));

      // center the setup wizard
      Point location = null;
      Dimension parentSize = null;
      if(parent.isVisible())
      {
         location = parent.getLocation();
         parentSize = parent.getSize();
      }
      else
      {
         location = new Point(0,0);
         parentSize = Toolkit.getDefaultToolkit().getScreenSize();
      }
      Dimension size = getSize();
      location.translate((parentSize.width-size.width)/2,
                         (parentSize.height-size.height)/2);
      setLocation(location);

      // highlight the "next" button
      getRootPane().setDefaultButton(next);
   }

   public boolean isCanceled()
   {
      return isCanceled;
   }

   private void onFinish()
   {
      ViewerProperties.setProperty(OneWireViewer.ADAPTER_NAME, ac.getAdapterName());
      ViewerProperties.setProperty(OneWireViewer.ADAPTER_PORT, ac.getAdapterPort());

      int index = 0;
      for(; index<pollRateButtons.length; index++)
      {
         if(pollRateButtons[index].isSelected())
            break;
      }
      ViewerProperties.setPropertyInt(OneWireViewer.POLLING_RATE_INDEX,
            (index==pollRateButtons.length) ? 0 : index);

      ViewerProperties.setPropertyBoolean(OneWireViewer.ENABLE_NORMAL_SEARCHING,
                                          normalModeEnabled.isSelected());
      ViewerProperties.setPropertyBoolean(OneWireViewer.ENABLE_TAG_SEARCHING,
                                          taggingModeEnabled.isSelected());
      this.setVisible(false);
      this.dispose();
   }

   private void onCancel()
   {
      isCanceled = true;

      this.setVisible(false);
      this.dispose();
   }

   private void onNext()
   {
      currentIndex++;
      if(currentIndex>MAX_INDEX)
      {
         currentIndex--;
      }
      setCenterPanel();
   }

   private void onPrev()
   {
      currentIndex--;
      if(currentIndex<MIN_INDEX)
      {
         currentIndex++;
      }
      setCenterPanel();
   }

   public void actionPerformed(ActionEvent ae)
   {
      if(ae.getSource()==cancel)
         onCancel();
      else if(ae.getSource()==prev)
            onPrev();
      else if(ae.getSource()==next)
      {
         if(validateCurrentPage())
         {
            if(next.getText().equals("Finish"))
               onFinish();
            else
               onNext();
         }
      }
   }

   private JPanel getPage0()
   {
      if(ac==null)
      {
         ac = new AdapterChooser(
                 OneWireAccessProvider.getProperty("onewire.adapter.default"),
                 OneWireAccessProvider.getProperty("onewire.port.default"),
                 fontBold, fontPlain);
         ac.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "1-Wire Adapter Port"));
      }
      return ac;
   }

   private boolean validatePage0()
   {
      String adapterName = ac.getAdapterName();
      String adapterPort = ac.getAdapterPort();
      if(adapterName.length()>0 && adapterPort.length()>0)
      {
         DSPortAdapter newAdapter = null;
         try
         {
            newAdapter =
               OneWireAccessProvider.getAdapter(adapterName, adapterPort);
            if(newAdapter.adapterDetected())
               return true;

            JOptionPane.showMessageDialog(this,
               new JLabel("No adapter detected on the specified port"),
               "No adapter detected",
               JOptionPane.ERROR_MESSAGE);
         }
         catch(Exception e)
         {
            JOptionPane.showMessageDialog(this,
               new JLabel(e.getMessage()), "Error loading specified adapter",
               JOptionPane.ERROR_MESSAGE);
         }
         finally
         {
            try
            {
               newAdapter.freePort();
            }
            catch(Exception e) {;}
         }
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            new JLabel("You must specify a valid adapter/port combination."),
            "No adapter/port specified",
            JOptionPane.ERROR_MESSAGE);
      }
      return false;
   }

   private JPanel getPage1()
   {
      if(pollRatePanel==null)
      {
         pollRatePanel = new JPanel(new BorderLayout());
         pollRatePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Device Polling Rate"));

         JTextArea textArea = new JTextArea();
         textArea.setEditable(false);
         textArea.setBackground(pollRatePanel.getBackground());
         textArea.setForeground(pollRatePanel.getForeground());
         textArea.setFont(fontPlain);
         textArea.setWrapStyleWord(true);
         textArea.setText(
            "This selection determines the amount of time before the\n" +
            "status of each device is re-checked.  In other words, if\n" +
            "the device polling rate is set to 5 seconds, there will\n" +
            "be a period of at least 5 seconds before a temperature\n" +
            "conversion command is sent to a temperature iButton such\n" +
            "as the DS18B20.");
         pollRatePanel.add(textArea, BorderLayout.NORTH);

         ButtonGroup pollRateButtonGroup = new ButtonGroup();
         pollRateButtons = new JCheckBoxMenuItem[6];
         pollRateButtons[0] = new JCheckBoxMenuItem("1 Second");
         pollRateButtons[0].setActionCommand("1000");
         pollRateButtons[0].setSelected(true);
         pollRateButtons[1] = new JCheckBoxMenuItem("5 Seconds");
         pollRateButtons[1].setActionCommand("5000");
         pollRateButtons[2] = new JCheckBoxMenuItem("10 Seconds");
         pollRateButtons[2].setActionCommand("10000");
         pollRateButtons[3] = new JCheckBoxMenuItem("30 Seconds");
         pollRateButtons[3].setActionCommand("30000");
         pollRateButtons[4] = new JCheckBoxMenuItem("1 Minute");
         pollRateButtons[4].setActionCommand("60000");
         pollRateButtons[5] = new JCheckBoxMenuItem("5 Minutes");
         pollRateButtons[5].setActionCommand("300000");

         JPanel pollRateOptionsPanel = new JPanel(new GridLayout(6, 1, 3, 3));
         pollRateOptionsPanel.setBorder(BorderFactory.createEtchedBorder());
         for(int i=0; i<pollRateButtons.length; i++)
         {
            pollRateButtons[i].setFont(fontBold);
            pollRateButtonGroup.add(pollRateButtons[i]);
            pollRateOptionsPanel.add(pollRateButtons[i]);
         }
         JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
         flowPanel.add(pollRateOptionsPanel);
         pollRatePanel.add(flowPanel, BorderLayout.CENTER);
      }
      return pollRatePanel;
   }

   private JPanel getPage2()
   {
      if(searchModePanel==null)
      {
         searchModePanel = new JPanel(new BorderLayout());
         searchModePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "1-Wire Search Mode"));

         JTextArea textArea = new JTextArea();
         textArea.setEditable(false);
         textArea.setBackground(pollRatePanel.getBackground());
         textArea.setForeground(pollRatePanel.getForeground());
         textArea.setFont(fontPlain);
         textArea.setWrapStyleWord(true);
         textArea.setText(
            "The OneWireViewer application can search for all devices\n" +
            "on the 1-Wire network or it can search for XML Tagged\n" +
            "devices.  If a device is tagged, it will be displayed with\n" +
            "a meaningful label, rather than it's device address.  In\n" +
            "addition, only the functionality associated with its tag\n" +
            "will be accessible for that device.");
         searchModePanel.add(textArea, BorderLayout.NORTH);


         JPanel checkboxGrid = new JPanel(new GridLayout(2, 1, 3, 3));
         checkboxGrid.setBorder(BorderFactory.createEtchedBorder());
         normalModeEnabled = new JCheckBox("Show Normal Devices");
         normalModeEnabled.setFont(fontBold);
         normalModeEnabled.setSelected(true);
         checkboxGrid.add(normalModeEnabled);
         taggingModeEnabled = new JCheckBox("Show Tagged Devices");
         taggingModeEnabled.setFont(fontBold);
         taggingModeEnabled.setSelected(false);
         checkboxGrid.add(taggingModeEnabled);
         JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
         flowPanel.add(checkboxGrid);
         searchModePanel.add(flowPanel, BorderLayout.CENTER);
      }
      return searchModePanel;
   }

   private boolean validatePage2()
   {
      if(!taggingModeEnabled.isSelected() && !normalModeEnabled.isSelected())
      {
         JOptionPane.showMessageDialog(this,
            new JLabel("At least one search mode option should be selected."),
            "No option selected",
            JOptionPane.ERROR_MESSAGE);
         return false;
      }
      return true;
   }

   private void setCenterPanel()
   {
      if(comp!=null)
         contentPane.remove(comp);
      switch(currentIndex)
      {
         default:
         case 0:
            comp = getPage0();
            next.setText("Next >>");
            break;
         case 1:
            comp = getPage1();
            break;
         case 2:
            comp = getPage2();
            // this is the last page, so change next to finish
            next.setText("Finish");
            break;
      }
      contentPane.add(comp, BorderLayout.CENTER);
      contentPane.validate();
      contentPane.repaint();
   }

   private boolean validateCurrentPage()
   {
      switch(currentIndex)
      {
         case 0:
            return validatePage0();
         case 2:
            return validatePage2();
         default:
            return true;
      }
   }

   private JPanel getButtonPanel()
   {
      JPanel helpButtonPanel = new JPanel(new FlowLayout());
      helpButtonPanel.add(help = new JButton("Help"));

      JPanel buttons = new JPanel(new FlowLayout());
      buttons.add(prev = new JButton("<< Previous"));
      buttons.add(next = new JButton("Next >>"));
      buttons.add(cancel = new JButton("Cancel"));

      help.addActionListener(this);
      help.setFont(fontBold);
      prev.addActionListener(this);
      prev.setFont(fontBold);
      next.addActionListener(this);
      next.setFont(fontBold);
      cancel.addActionListener(this);
      cancel.setFont(fontBold);

      JPanel buttonPanel = new JPanel(new BorderLayout());
      buttonPanel.setPreferredSize(new Dimension(640, 40));
      buttonPanel.add(helpButtonPanel, BorderLayout.WEST);
      buttonPanel.add(buttons, BorderLayout.CENTER);
      buttonPanel.setBorder(BorderFactory.createEtchedBorder());
      return buttonPanel;
   }

   private JPanel getSideBarPanel()
   {
      JPanel sideBar = new JPanel(new BorderLayout(3,3));
      ClassLoader cl = this.getClass().getClassLoader();
      ImageIcon imgIcon = new ImageIcon(cl.getResource("images/ibutton.jpg"));
      BufferedImage buffImg = new BufferedImage(imgIcon.getIconWidth(),
                                                imgIcon.getIconHeight(),
                                                BufferedImage.TYPE_4BYTE_ABGR);
      imgIcon.paintIcon(sideBar, buffImg.createGraphics(), 0, 0);


      JPanel p = new PicturePanel(buffImg);
      p.setBorder(BorderFactory.createEtchedBorder());
      p.setLayout(new GridLayout(8,1));

      JLabel sideLbl = new JLabel("1-Wire API", JLabel.CENTER);
      sideLbl.setFont(fontBold);
      sideLbl.setForeground(Color.black);
      sideLbl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
      p.add(sideLbl);
      JLabel sideLblV = new JLabel("Setup v1.00", JLabel.CENTER);
      sideLblV.setForeground(Color.black);
      sideLblV.setFont(fontBold);
      p.add(sideLblV);

      p.setPreferredSize(new Dimension(140, 200));
      return p;
   }

   private JLabel getLogo()
   {
      BufferedImage buffImg = new BufferedImage(100, 70,
                                                BufferedImage.TYPE_4BYTE_ABGR);
      Graphics2D g2d = buffImg.createGraphics();
      g2d.setColor(Color.black);
      g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
      g2d.drawOval(16,2,66,66);
      g2d.drawLine(38, 59, 57, 59);
      Font times = new Font("Times New Roman", Font.BOLD, 68);
      g2d.setFont(times);
      g2d.drawString("i", 39, 56);

      return new JLabel(new ImageIcon(buffImg));
   }

   public static void main(String[] args)
   {
      final JFrame parent = new JFrame();
      final OneWireSetup oneWireSetup = new OneWireSetup(parent, true);
      oneWireSetup.addWindowListener(new WindowAdapter()
         {
            public void windowClosing(WindowEvent we)
            {
               oneWireSetup.onCancel();
               parent.dispose();
               System.exit(0);
            }
         }
      );
      oneWireSetup.setVisible(true);
   }
}

class PicturePanel extends JPanel
{
   BufferedImage img = null;
   Paint paint = null;
   public PicturePanel(BufferedImage i)
   {
      img = i;
      paint = new TexturePaint(img,
                               new Rectangle(0,0,img.getWidth(),img.getHeight()));
   }

   public void paint(Graphics g)
   {
      Graphics2D g2d = (Graphics2D)g;
      Paint p = g2d.getPaint();
      Composite c = g2d.getComposite();
      g2d.setPaint(paint);
      Dimension size = this.getSize();
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .15f));
      g2d.fillRect(0,0,size.width, size.height);
      g2d.setPaint(p);
      g2d.setComposite(c);
      paintChildren(g);
   }

   public void update(Graphics g)
   {
      paint(g);
   }
}
