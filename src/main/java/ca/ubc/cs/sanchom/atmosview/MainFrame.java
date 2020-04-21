package ca.ubc.cs.sanchom.atmosview;

import com.toedter.calendar.JDateChooser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

public class MainFrame extends JFrame {

  private static final long serialVersionUID = 1L;

  private JPanel ButtonPanel = null;

  private JPanel jContentPane = null;

  private JTabbedPane jTabbedPane = null;

  private JScrollPane stationScroller = null;

  private JList stationJList = null;

  //  @jve:decl-index=0:
  private Vector<String> stationIDs = null;

  private JButton getDataButton = null;

  private SoundingPanel SoundingDisplayPanel = null;

  private BarPanel BarDisplayPanel = null;

  private Box NorthPanel = null;

  private JPanel SingleViewPanel = null;

  private JPanel MultiViewPanel = null;

  private JButton ActivateFileChooserButton = null;

  private JDateChooser DateChooser = null;

  private JSplitPane SplitPane = null;

  //  @jve:decl-index=0:
  private ButtonGroup HourRadios = null;

  private JRadioButton zeroButton = null;

  private JRadioButton twelveButton = null;

  private LegendPanel legendPanel = null;

  private Vector<BarPanel> multiples = null;

  private double splitRatio = 0;

  private class FileExtensionFilter extends FileFilter {
    private String extension = null;

    public FileExtensionFilter(String extension) {
      super();
      this.extension = extension;
    }

    public boolean accept(File file) {
      return file.getName().endsWith("." + extension);
    }

    public String getDescription() {
      return extension + " files";
    }
  }

  /** This is the default constructor */
  public MainFrame() {
    super();
    initialize();
    multiples = new Vector<BarPanel>();
  }

  /** This method initializes this */
  private void initialize() {
    this.setSize(1000, 700);
    this.setContentPane(getJContentPane());
    this.setTitle("AtmosView");
    this.addWindowListener(
        new java.awt.event.WindowAdapter() {
          public void windowClosing(java.awt.event.WindowEvent e) {
            System.exit(0);
          }
        });
  }

  /**
   * This method initializes jContentPane
   *
   * @return javax.swing.JPanel
   */
  private JPanel getJContentPane() {
    if (jContentPane == null) {
      jContentPane = new JPanel();
      jContentPane.setLayout(new BoxLayout(getJContentPane(), BoxLayout.Y_AXIS));
      jContentPane.add(getNorthPanel());
      jContentPane.add(getJTabbedPane(), null);
    }
    return jContentPane;
  }

  /**
   * This method initializes jTabbedPane
   *
   * @return javax.swing.JTabbedPane
   */
  private JTabbedPane getJTabbedPane() {
    if (jTabbedPane == null) {
      jTabbedPane = new JTabbedPane();
      jTabbedPane.addTab(
          "Single View", null, getSingleViewPanel(), "Select and view individual soundings");
      jTabbedPane.addTab(
          "Multi View", null, getMultiViewPanel(), "Select and view small multiples of soundings");
    }
    return jTabbedPane;
  }

  private Box getNorthPanel() {
    if (NorthPanel == null) {
      NorthPanel = Box.createHorizontalBox();
      NorthPanel.setMaximumSize(new Dimension(1000, 350));
      NorthPanel.add(Box.createHorizontalGlue());
      NorthPanel.add(getStationScroller(), null);
      NorthPanel.add(Box.createHorizontalGlue());
      NorthPanel.add(getButtonPanel(), null);
      NorthPanel.add(Box.createHorizontalGlue());
      NorthPanel.add(getLegendPanel(), null);
      NorthPanel.add(Box.createHorizontalGlue());
    }
    return NorthPanel;
  }

  private JPanel getSingleViewPanel() {
    if (SingleViewPanel == null) {
      SingleViewPanel = new JPanel();
      SingleViewPanel.setLayout(new BoxLayout(getSingleViewPanel(), BoxLayout.X_AXIS));
      SingleViewPanel.add(getSplitPane());
    }
    return SingleViewPanel;
  }

  private JPanel getMultiViewPanel() {
    if (MultiViewPanel == null) {
      MultiViewPanel = new JPanel();
      MultiViewPanel.setLayout(new BoxLayout(getMultiViewPanel(), BoxLayout.X_AXIS));

      MultiViewPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              redrawMultiples();
            }
          });
    }
    return MultiViewPanel;
  }

  /**
   * This methoed initializes dataSourceButtonPanel
   *
   * @return javax.swing.JPanel
   */
  private JPanel getButtonPanel() {
    if (ButtonPanel == null) {
      ButtonPanel = new JPanel();
      ButtonPanel.setLayout(new BoxLayout(getButtonPanel(), BoxLayout.Y_AXIS));
      ButtonPanel.setMaximumSize(new Dimension(200, 350));
      ButtonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
      ButtonPanel.add(getDateChooser());
      ButtonPanel.add(getZeroButton());
      ButtonPanel.add(getTwelveButton());
      ButtonPanel.add(getGetDataButton());
      ButtonPanel.add(getActivateFileChooserButton());
      ButtonPanel.add(Box.createVerticalGlue());
    }

    return ButtonPanel;
  }

  private LegendPanel getLegendPanel() {
    if (legendPanel == null) {
      legendPanel = new LegendPanel();
      legendPanel.setBackground(Color.WHITE);
      legendPanel.setPreferredSize(new Dimension(300, 150));
      legendPanel.setBorder(BorderFactory.createBevelBorder(1));
      legendPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              legendPanel.updateShapes();
            }
          });
    }
    return legendPanel;
  }

  /**
   * This method initializes stationScroller
   *
   * @return javax.swing.JScrollPane
   */
  private JScrollPane getStationScroller() {
    if (stationScroller == null) {
      stationScroller = new JScrollPane();
      stationScroller.setMaximumSize(new Dimension(350, 150));
      stationScroller.setBorder(
          BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Available Soundings"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
      stationScroller.setViewportView(getStationList());
    }
    return stationScroller;
  }

  /**
   * This method initializes stationJList
   *
   * @return javax.swing.JList
   */
  private JList getStationList() {
    if (stationJList == null) {
      stationJList = new JList();
      stationJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      stationJList.addMouseListener(
          new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
              if (e.getClickCount() == 2) {
                downloadSounding();
              }
            }
          });

      TreeMap stationList = null;
      try {
        stationList = SoundingFetcher.getInstance().getStationList();
      } catch (IOException e) {
        stationList = new TreeMap();
      }
      Vector<String> listData = new Vector<String>();
      stationIDs = new Vector<String>();

      Iterator i = stationList.entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry e = (Map.Entry) i.next();
        String stationName = (String) (e.getKey());
        String stationID = (String) (e.getValue());
        stationIDs.add(stationID);
        listData.add(stationName + "\t" + stationID);
      }

      stationJList.setListData(listData);
    }
    return stationJList;
  }

  /**
   * This method initializes getDataButton
   *
   * @return javax.swing.JButton
   */
  private JButton getGetDataButton() {
    if (getDataButton == null) {
      getDataButton = new JButton();
      getDataButton.setText("Download");
      getDataButton.setMaximumSize(new Dimension(200, 25));
      getDataButton.setAlignmentX(RIGHT_ALIGNMENT);
      getDataButton.addActionListener(
          new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {

              downloadSounding();
            }
          });
    }
    return getDataButton;
  }

  private JButton getActivateFileChooserButton() {
    if (ActivateFileChooserButton == null) {
      ActivateFileChooserButton = new JButton();
      ActivateFileChooserButton.setText("Choose Local File");
      ActivateFileChooserButton.setAlignmentX(RIGHT_ALIGNMENT);
      ActivateFileChooserButton.setMaximumSize(new Dimension(200, 25));
      final Component parent = this;
      ActivateFileChooserButton.addActionListener(
          new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
              JFileChooser chooser = new JFileChooser();
              chooser.setFileFilter(new FileExtensionFilter("csv"));
              // TODO: fix this!
              chooser.setCurrentDirectory(new File("/Users/sancho/Documents/Projects/atmospheric"));
              int returnVal = chooser.showOpenDialog(parent);
              try {

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                  Cursor orig = jContentPane.getCursor();
                  jContentPane.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                  SoundingData data = SoundingCSVParser.getSounding(chooser.getSelectedFile());

                  SoundingDisplayPanel.linkSoundingData(data);
                  BarDisplayPanel.linkSoundingData(data);
                  SoundingDisplayPanel.repaint();
                  BarDisplayPanel.repaint();
                  jContentPane.setCursor(orig);
                }
              } catch (Exception ex) {

              }
            }
          });
    }
    return ActivateFileChooserButton;
  }

  private SoundingPanel getSoundingPanel() {
    if (SoundingDisplayPanel == null) {
      SoundingDisplayPanel = new SoundingPanel();
      SoundingDisplayPanel.setBackground(Color.WHITE);
      SoundingDisplayPanel.setSize(new Dimension(400, 550));

      SoundingDisplayPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              SoundingDisplayPanel.updateShapes();
            }
          });
    }

    return SoundingDisplayPanel;
  }

  private JSplitPane getSplitPane() {
    if (SplitPane == null) {
      SplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
      SplitPane.setLeftComponent(getVisualizationPanel());
      SplitPane.setRightComponent(getSoundingPanel());
      SplitPane.setDividerLocation(500);
      splitRatio = 0.5;
      final JSplitPane j = SplitPane;
      SplitPane.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              j.setDividerLocation((int) (splitRatio * j.getWidth()));
            }
          });
      SplitPane.addPropertyChangeListener(
          "dividerLocation",
          new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
              splitRatio = (float) ((Integer) e.getNewValue()) / (float) j.getWidth();
            }
          });
    }
    return SplitPane;
  }

  private BarPanel getVisualizationPanel() {
    if (BarDisplayPanel == null) {
      BarDisplayPanel = new BarPanel();
      BarDisplayPanel.setBackground(Color.WHITE);
      BarDisplayPanel.setSize(new Dimension(300, 550));
      BarDisplayPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              BarDisplayPanel.updateShapes();
            }
          });
    }

    return BarDisplayPanel;
  }

  private ButtonGroup getHourGroup() {
    if (HourRadios == null) {
      HourRadios = new ButtonGroup();

      JRadioButton twelveButton = new JRadioButton("12:00 UTC");
      twelveButton.setActionCommand("12");

      HourRadios.add(twelveButton);
    }
    return HourRadios;
  }

  private JRadioButton getZeroButton() {
    if (zeroButton == null) {
      zeroButton = new JRadioButton("00:00 UTC");
      zeroButton.setActionCommand("0");
      zeroButton.setSelected(true);
      zeroButton.setAlignmentX(RIGHT_ALIGNMENT);

      getHourGroup().add(zeroButton);
    }
    return zeroButton;
  }

  private JRadioButton getTwelveButton() {
    if (twelveButton == null) {
      twelveButton = new JRadioButton("12:00 UTC");
      twelveButton.setActionCommand("12");
      twelveButton.setAlignmentX(RIGHT_ALIGNMENT);

      getHourGroup().add(twelveButton);
    }
    return twelveButton;
  }

  private JDateChooser getDateChooser() {
    if (DateChooser == null) {
      DateChooser = new JDateChooser(new Date());
      DateChooser.setAlignmentX(RIGHT_ALIGNMENT);
      DateChooser.setPreferredSize(new Dimension(200, 30));
      DateChooser.setMaximumSize(new Dimension(200, 30));
      DateChooser.getCalendarButton().setIcon(null);
      DateChooser.getCalendarButton().setText("Calendar");
    }
    return DateChooser;
  }

  private void downloadSounding() {
    Cursor orig = jContentPane.getCursor();
    jContentPane.setCursor(new Cursor(Cursor.WAIT_CURSOR));
    int selectedIndex = stationJList.getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < stationIDs.size()) {
      GregorianCalendar chosen = new GregorianCalendar();
      chosen.setTime(DateChooser.getDate());

      chosen.set(Calendar.HOUR, Integer.parseInt(HourRadios.getSelection().getActionCommand()));

      SoundingData soundingData = null;
      try {
        soundingData =
            SoundingFetcher.getInstance().getSounding(stationIDs.get(selectedIndex), chosen);
      } catch (IOException e) {
        System.err.println(e);
      }

      if (soundingData != null) {
        if (jTabbedPane.getSelectedIndex() == 0) {
          SoundingDisplayPanel.linkSoundingData(soundingData);
          BarDisplayPanel.linkSoundingData(soundingData);
          SoundingDisplayPanel.repaint();
          BarDisplayPanel.repaint();
        } else {
          if (multiples.size() < 8) {
            final BarPanel b = new BarPanel();
            b.setBackground(Color.WHITE);
            b.setBorder(BorderFactory.createLoweredBevelBorder());

            b.addMouseListener(
                new java.awt.event.MouseAdapter() {
                  public void mouseClicked(java.awt.event.MouseEvent e) {
                    int mask = java.awt.event.MouseEvent.BUTTON1_MASK - 1;
                    int mods = e.getModifiers() & mask;
                    if (mods != 0) // Right button clicked, or ctrl click
                    {
                      multiples.remove(b);
                      redrawMultiples();
                    } else if (e.getClickCount() == 2) {
                      showInSingleView(b.getSoundingData());
                    }
                  }
                });

            b.linkSoundingData(soundingData);
            multiples.add(b);

            redrawMultiples();
          } // end if less than 8 multiples
        } // if-else for single-view vs multi-view
      } // end if soundingData != null
    }
    jContentPane.setCursor(orig);
  }

  private void redrawMultiples() {
    MultiViewPanel.removeAll();

    if (multiples.size() > 4) {
      MultiViewPanel.setLayout(new BoxLayout(MultiViewPanel, BoxLayout.Y_AXIS));
    } else {
      MultiViewPanel.setLayout(new BoxLayout(MultiViewPanel, BoxLayout.X_AXIS));
    }

    JPanel topRow = new JPanel();
    topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
    JPanel bottomRow = new JPanel();
    bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.X_AXIS));

    Iterator<BarPanel> i = multiples.iterator();
    int num = 0;
    while (i.hasNext()) {
      BarPanel n = i.next();
      int dim =
          Math.min(
              MultiViewPanel.getHeight() / (multiples.size() <= 4 ? 1 : 2),
              MultiViewPanel.getWidth() / Math.min(multiples.size(), 4));
      n.setMaximumSize(new Dimension(dim, dim));
      n.setSize(new Dimension(dim, dim));

      if (num < 4) topRow.add(n);
      else bottomRow.add(n);
      num++;
    }

    MultiViewPanel.add(topRow);
    MultiViewPanel.add(bottomRow);

    i = multiples.iterator();
    while (i.hasNext()) {
      BarPanel n = i.next();
      n.updateShapes();
      n.repaint();
    }
    jContentPane.repaint();
  }

  private void showInSingleView(SoundingData data) {
    SoundingDisplayPanel.linkSoundingData(data);
    BarDisplayPanel.linkSoundingData(data);
    SoundingDisplayPanel.repaint();
    BarDisplayPanel.repaint();
    jTabbedPane.setSelectedIndex(0);
  }
}
