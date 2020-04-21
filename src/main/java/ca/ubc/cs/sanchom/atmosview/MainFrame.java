package ca.ubc.cs.sanchom.atmosview;

import com.toedter.calendar.JDateChooser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

public class MainFrame extends JFrame {

  private static final long serialVersionUID = 1L;

  private JPanel buttonPanel = null;

  private JPanel contentPanel = null;

  private JTabbedPane tabbedPane = null;

  private JScrollPane stationScroller = null;

  private JList stationJList = null;

  //  @jve:decl-index=0:
  private Vector<String> stationIDs = null;

  private JButton getDataButton = null;

  private SoundingPanel soundingDisplayPanel = null;

  private BarPanel barDisplayPanel = null;

  private Box northPanel = null;

  private JPanel singleViewPanel = null;

  private JPanel multieViewPanel = null;

  private JButton activateFileChooserButton = null;

  private JDateChooser dateChooser = null;

  private JSplitPane splitPane = null;

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

  /** This is the default constructor. */
  public MainFrame() {
    super();
    initialize();
    multiples = new Vector<BarPanel>();
  }

  /** This method initializes this. */
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
   * This method initializes contentPanel.
   *
   * @return javax.swing.JPanel
   */
  private JPanel getJContentPane() {
    if (contentPanel == null) {
      contentPanel = new JPanel();
      contentPanel.setLayout(new BoxLayout(getJContentPane(), BoxLayout.Y_AXIS));
      contentPanel.add(getNorthPanel());
      contentPanel.add(getJTabbedPane(), null);
    }
    return contentPanel;
  }

  /**
   * This method initializes tabbedPane.
   *
   * @return javax.swing.JTabbedPane
   */
  private JTabbedPane getJTabbedPane() {
    if (tabbedPane == null) {
      tabbedPane = new JTabbedPane();
      tabbedPane.addTab(
          "Single View", null, getSingleViewPanel(), "Select and view individual soundings");
      tabbedPane.addTab(
          "Multi View", null, getMultiViewPanel(), "Select and view small multiples of soundings");
    }
    return tabbedPane;
  }

  private Box getNorthPanel() {
    if (northPanel == null) {
      northPanel = Box.createHorizontalBox();
      northPanel.setMaximumSize(new Dimension(1000, 350));
      northPanel.add(Box.createHorizontalGlue());
      northPanel.add(getStationScroller(), null);
      northPanel.add(Box.createHorizontalGlue());
      northPanel.add(getButtonPanel(), null);
      northPanel.add(Box.createHorizontalGlue());
      northPanel.add(getLegendPanel(), null);
      northPanel.add(Box.createHorizontalGlue());
    }
    return northPanel;
  }

  private JPanel getSingleViewPanel() {
    if (singleViewPanel == null) {
      singleViewPanel = new JPanel();
      singleViewPanel.setLayout(new BoxLayout(getSingleViewPanel(), BoxLayout.X_AXIS));
      singleViewPanel.add(getSplitPane());
    }
    return singleViewPanel;
  }

  private JPanel getMultiViewPanel() {
    if (multieViewPanel == null) {
      multieViewPanel = new JPanel();
      multieViewPanel.setLayout(new BoxLayout(getMultiViewPanel(), BoxLayout.X_AXIS));

      multieViewPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              redrawMultiples();
            }
          });
    }
    return multieViewPanel;
  }

  /**
   * This methoed initializes dataSourceButtonPanel.
   *
   * @return javax.swing.JPanel
   */
  private JPanel getButtonPanel() {
    if (buttonPanel == null) {
      buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(getButtonPanel(), BoxLayout.Y_AXIS));
      buttonPanel.setMaximumSize(new Dimension(200, 350));
      buttonPanel.add(Box.createRigidArea(new Dimension(0, 20)));
      buttonPanel.add(getDateChooser());
      buttonPanel.add(getZeroButton());
      buttonPanel.add(getTwelveButton());
      buttonPanel.add(getGetDataButton());
      buttonPanel.add(getActivateFileChooserButton());
      buttonPanel.add(Box.createVerticalGlue());
    }

    return buttonPanel;
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
   * This method initializes stationScroller.
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
   * This method initializes stationJList.
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
   * This method initializes getDataButton.
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
    if (activateFileChooserButton == null) {
      activateFileChooserButton = new JButton();
      activateFileChooserButton.setText("Choose Local File");
      activateFileChooserButton.setAlignmentX(RIGHT_ALIGNMENT);
      activateFileChooserButton.setMaximumSize(new Dimension(200, 25));
      final Component parent = this;
      final JFrame frame = this;
      activateFileChooserButton.addActionListener(
          new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
              JFileChooser chooser = new JFileChooser();
              chooser.setFileFilter(new FileExtensionFilter("csv"));
              // chooser.setCurrentDirectory(new
              // File("/Users/sancho/Documents/Projects/atmospheric"));
              int returnVal = chooser.showOpenDialog(parent);
              if (returnVal == JFileChooser.APPROVE_OPTION) {
                Cursor orig = contentPanel.getCursor();
                contentPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                try {
                  SoundingData data = SoundingCsvParser.getSounding(chooser.getSelectedFile());
                  soundingDisplayPanel.linkSoundingData(data);
                  barDisplayPanel.linkSoundingData(data);
                  soundingDisplayPanel.repaint();
                  barDisplayPanel.repaint();
                } catch (FileNotFoundException fnne) {
                  JOptionPane.showMessageDialog(
                      frame, fnne.getMessage(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                  return;
                } catch (IOException ioe) {
                  JOptionPane.showMessageDialog(
                      frame, ioe.getMessage(), "Error Reading File", JOptionPane.ERROR_MESSAGE);
                  return;
                } finally {
                  contentPanel.setCursor(orig);
                }
              }
            }
          });
    }
    return activateFileChooserButton;
  }

  private SoundingPanel getSoundingPanel() {
    if (soundingDisplayPanel == null) {
      soundingDisplayPanel = new SoundingPanel();
      soundingDisplayPanel.setBackground(Color.WHITE);
      soundingDisplayPanel.setSize(new Dimension(400, 550));

      soundingDisplayPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              soundingDisplayPanel.updateShapes();
            }
          });
    }

    return soundingDisplayPanel;
  }

  private JSplitPane getSplitPane() {
    if (splitPane == null) {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
      splitPane.setLeftComponent(getVisualizationPanel());
      splitPane.setRightComponent(getSoundingPanel());
      splitPane.setDividerLocation(500);
      splitRatio = 0.5;
      final JSplitPane j = splitPane;
      splitPane.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              j.setDividerLocation((int) (splitRatio * j.getWidth()));
            }
          });
      splitPane.addPropertyChangeListener(
          "dividerLocation",
          new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
              splitRatio = (float) ((Integer) e.getNewValue()) / (float) j.getWidth();
            }
          });
    }
    return splitPane;
  }

  private BarPanel getVisualizationPanel() {
    if (barDisplayPanel == null) {
      barDisplayPanel = new BarPanel();
      barDisplayPanel.setBackground(Color.WHITE);
      barDisplayPanel.setSize(new Dimension(300, 550));
      barDisplayPanel.addComponentListener(
          new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
              barDisplayPanel.updateShapes();
            }
          });
    }

    return barDisplayPanel;
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
    if (dateChooser == null) {
      dateChooser = new JDateChooser(new Date());
      dateChooser.setAlignmentX(RIGHT_ALIGNMENT);
      dateChooser.setPreferredSize(new Dimension(200, 30));
      dateChooser.setMaximumSize(new Dimension(200, 30));
      dateChooser.getCalendarButton().setIcon(null);
      dateChooser.getCalendarButton().setText("Calendar");
    }
    return dateChooser;
  }

  private void downloadSounding() {
    Cursor orig = contentPanel.getCursor();
    contentPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
    int selectedIndex = stationJList.getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < stationIDs.size()) {
      GregorianCalendar chosen = new GregorianCalendar();
      chosen.setTime(dateChooser.getDate());

      chosen.set(Calendar.HOUR, Integer.parseInt(HourRadios.getSelection().getActionCommand()));

      SoundingData soundingData = null;
      try {
        soundingData =
            SoundingFetcher.getInstance().getSounding(stationIDs.get(selectedIndex), chosen);
      } catch (IOException e) {
        System.err.println(e);
      }

      if (soundingData != null) {
        if (tabbedPane.getSelectedIndex() == 0) {
          soundingDisplayPanel.linkSoundingData(soundingData);
          barDisplayPanel.linkSoundingData(soundingData);
          soundingDisplayPanel.repaint();
          barDisplayPanel.repaint();
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
                    // Right button clicked, or ctrl click
                    if (mods != 0) {
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
    contentPanel.setCursor(orig);
  }

  private void redrawMultiples() {
    multieViewPanel.removeAll();

    if (multiples.size() > 4) {
      multieViewPanel.setLayout(new BoxLayout(multieViewPanel, BoxLayout.Y_AXIS));
    } else {
      multieViewPanel.setLayout(new BoxLayout(multieViewPanel, BoxLayout.X_AXIS));
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
              multieViewPanel.getHeight() / (multiples.size() <= 4 ? 1 : 2),
              multieViewPanel.getWidth() / Math.min(multiples.size(), 4));
      n.setMaximumSize(new Dimension(dim, dim));
      n.setSize(new Dimension(dim, dim));

      if (num < 4) {
        topRow.add(n);
      } else {
        bottomRow.add(n);
      }
      num++;
    }

    multieViewPanel.add(topRow);
    multieViewPanel.add(bottomRow);

    i = multiples.iterator();
    while (i.hasNext()) {
      BarPanel n = i.next();
      n.updateShapes();
      n.repaint();
    }
    contentPanel.repaint();
  }

  private void showInSingleView(SoundingData data) {
    soundingDisplayPanel.linkSoundingData(data);
    barDisplayPanel.linkSoundingData(data);
    soundingDisplayPanel.repaint();
    barDisplayPanel.repaint();
    tabbedPane.setSelectedIndex(0);
  }
}
