package com.company;
import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.FileOutputStream;
import java.util.*;
import java.awt.event.*;
import java.io.*;

public class Main
{
    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    //массив флажков
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames={"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
    "Accoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom",
    "Hi Bongo", "Maracas", "Whistle", "Low Conga",
    "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
    //список инструментов
    int[] instruments={35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
    //"барабанные" клавиши, где каждая клавиша - отдельный барабан
    public static void main(String[] args)
    {
        new Main().buildGUI();
    }
    public void buildGUI()
    {
        theFrame=new JFrame("БитБокс");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout=new BorderLayout();
        JPanel background=new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        //Пустая граница позволит создать поля между
        //краями панели и местом размещения компонентов

        checkboxList=new ArrayList<JCheckBox>();
        Box buttonBox=new Box(BoxLayout.Y_AXIS);

        JButton start=new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop=new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo=new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo=new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton serializelt=new JButton("Serializelt");
        serializelt.addActionListener(new MySendListener());
        buttonBox.add(serializelt);

        JButton restore=new JButton("Restore");
        restore.addActionListener(new MyReadInListener());
        buttonBox.add(restore);

        Box nameBox=new Box(BoxLayout.Y_AXIS);
        for(int i=0; i<16;i++)
        {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid=new GridLayout(16,16);
        //очередной диспетчер компановки;
        //разбивает панель на ячейки
        grid.setVgap(1);
        //расстояние между ячейками по вертикали
        grid.setHgap(2);
        //расстояние между ячейками по горизонтали
        mainPanel=new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for(int i=0;i<256;i++)
        {
            JCheckBox c=new JCheckBox();
            //создаем флажки
            c.setSelected(false);
            //присваиваем им значения false
            checkboxList.add(c);
            //добавляем флажки в массив
            mainPanel.add(c);
            //добавляем флажки на панель
        }

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi()
    {
        try
        {
            sequencer=MidiSystem.getSequencer();
            sequencer.open();
            sequence=new Sequence(Sequence.PPQ,4);
            track=sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart()
    {
        int[] trackList=null;
        //создаем массив из 16 эл-в, чтобы хранить значения для
        //каждого инструмента на все 16 тактов

        sequence.deleteTrack(track);
        track=sequence.createTrack();
        //удаляем старую дорожку и создаем новую

        for(int i=0;i<16;i++)
        {
            trackList = new int[16];

            int key = instruments[i];
            //задаем клавишу, которая представляет инструмент
            //массив содержит МИДИ-числа для каждого интструмента

            for (int j = 0; j < 16; j++)
            //для каждого такта текущего ряда
            {
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));

                //проверяем, установлен ли флажок на данном такте
                if (jc.isSelected()) {
                    trackList[j] = key;
                    //если да, то помещаем значение клавиши в текушую ячейку
                    //массва
                } else {
                    trackList[j] = 0;
                    //иначе инструмент не будет играть в данном такте
                }
            }
            makeTrack(trackList);
            track.add(makeEvent(176, 1, 127, 0, 16));
            //для этого и остальных 15 инструментов создаем события
            //и добавляем их на дорожку
        }
        track.add(makeEvent(192,9,1,0,15));
        //мы должны убедиться, что события на такте 16 существует (такты от
        //15 до 16), иначе БитБокс не пройдет все 16 тактов, перед тек,
        //как заново начать последовательность
        try
        {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            //позволяет задать кол-во повторений цикла или, кав этом
            //случае, непрерывный цикл
            sequencer.start();
            sequencer.setTempoInBPM(120);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //тепер мы проигрываем мелодию
    }

    public class MyStartListener implements ActionListener
    {
        public void actionPerformed(ActionEvent a)
        {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener
    {
        public void actionPerformed (ActionEvent a)
        {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener
    {
        public void actionPerformed(ActionEvent a)
        {
            float tempoFactor=sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor*1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener
    {
        public void actionPerformed(ActionEvent a)
        {
            float tempoFactor=sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor*.97));
        }
    }

    public void makeTrack(int[] list)
    {
        for(int i=0;i<16;i++)
        {
            int key=list[i];
            if(key!=0)
            {
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i+1));
            }
        }
    }

    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick)
    {
        MidiEvent event=null;
        try
        {
            ShortMessage a=new ShortMessage();
            a.setMessage(comd,chan,one,two);
            event=new MidiEvent(a,tick);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return event;
    }
    public class MySendListener implements ActionListener
        //класс для записи схемы
    {
        public void actionPerformed(ActionEvent a)
        {
            boolean[] checkboxState=new boolean[256];
            //массив для хранения состояния каждого флажка
            for (int i=0;i<256;i++)
            {
                JCheckBox check=(JCheckBox) checkboxList.get(i);
                if (check.isSelected())
                {
                    checkboxState[i]=true;
                }
            }
            try
            {
                FileNameExtensionFilter filter=new FileNameExtensionFilter("*.ser","*.*");
                JFileChooser savefile=new JFileChooser();
                savefile.setFileFilter(filter);
                int ret=savefile.showDialog(null, "Save file");
                if(ret==JFileChooser.APPROVE_OPTION)
                {
                    FileOutputStream fileStream = new FileOutputStream(savefile.getSelectedFile());
                    ObjectOutputStream os = new ObjectOutputStream(fileStream);
                    os.writeObject(checkboxState);
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
    public class MyReadInListener implements ActionListener
    {
        public void actionPerformed(ActionEvent a)
        {
            boolean[] checkboxState=null;
            try
            {
                JFileChooser fileopen=new JFileChooser();
                int ret=fileopen.showDialog(null, "Open file");
                //переменная ret позволяет узнать, что именно сделал пользователь
                if(ret==JFileChooser.APPROVE_OPTION)
                {
                    FileInputStream fileIn=new FileInputStream(fileopen.getSelectedFile());
                    ObjectInputStream is=new ObjectInputStream(fileIn);
                    checkboxState=(boolean[])is.readObject();
                    //Считываем объект из файла и определяем его как булев
                    //массив, т.к. readObject возвращает ссылку на тип Object
                }
                //если пользователь выбрал файл
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
            for(int i=0; i<256;i++)
            {
                JCheckBox check=(JCheckBox)checkboxList.get(i);
                if(checkboxState[i])
                {
                    check.setSelected(true);
                }
                else
                {
                    check.setSelected(false);
                }
            }
            //восстанавливаем состояние каждого флажка
            sequencer.stop();
            buildTrackAndStart();
        }
    }
}
