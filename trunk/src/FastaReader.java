import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.io.FastaReaderHelper;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import settings.PathManager;

public class FastaReader {
     int limit;
    File file;
     File sqliteFile;

    public FastaReader(String filename, int limit) throws Exception {
        this.limit = limit;
        this.file = new File(filename);

        if (fileType().equals("fasta"))
            FastaOpen();
        if (fileType().equals("nexus"));
    }

    private void FastaOpen() throws Exception {
        LinkedHashMap<String, ProteinSequence> a = FastaReaderHelper.readFastaProteinSequence(file);
        int count = 0;
        for (Entry<String, ProteinSequence> entry : a.entrySet()) {
            System.out.println( count + " of " +a.size() + " for " + file.getName());
            System.out.println("\tAccessionID = " + entry.getValue().getAccession().getID());
            System.out.println("\tSequence = " + entry.getValue().getSequenceAsString());
            count++;
            if (count > limit) break;
        }
    }


     public String fileType() {
         String filepath = file.getAbsolutePath();
        if (!file.exists())
            return null;

        int index = filepath.lastIndexOf('.');

        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);
            if (ext.equals("fasta"))
                return "fasta";
            if (ext.equals("nexus"))
                return "nexus";
        }

        return null;
    }


    public static void main(String[] args) throws Exception {
        FastaReader fastaReader = new FastaReader("sampledata/A.doe.CO1.fasta",2);

        // TODO:, see the following list to integrate FASTA sequence files into database
        // think about how to integrate this with larger process...
        // 1. assign BCID to accession ID, making a globally unique identifier
        // 2. assign some "hasSequence" property to the sequence itself
        // 3. create a triple file and upload to database (using upload script)
        // 4. create a sequence upload process (loop through files)

        /*
        new SequenceReader("sampledata/A.doe.CO1.fasta",2);
        new SequenceReader("sampledata/SophieVonderHeyden.fasta",2);
        new SequenceReader("sampledata/IriaFernandezSample.fasta",2);
        new SequenceReader("sampledata/RobToonenNexus.nexus",2);
        */
        /*
        String filename2 = "sampledata/A.doe.CR.fasta";
        String filename1 = "sampledata/A.doe.CO1.fasta";

        //Try with the FastaReaderHelper
        LinkedHashMap<String, ProteinSequence> a = FastaReaderHelper.readFastaProteinSequence(new File(filename1));
        //FastaReaderHelper.readFastaDNASequence for DNA sequences

        for (Entry<String, ProteinSequence> entry : a.entrySet()) {
            System.out.println(entry.getValue().getOriginalHeader() + "=" + entry.getValue().getSequenceAsString());
        }

        //Try reading with the FastaReader
        FileInputStream inStream = new FileInputStream(filename1);
        FastaReader<ProteinSequence, AminoAcidCompound> fastaReader =
                new FastaReader<ProteinSequence, AminoAcidCompound>(
                        inStream,
                        new GenericFastaHeaderParser<ProteinSequence, AminoAcidCompound>(),
                        new ProteinSequenceCreator(AminoAcidCompoundSet.getAminoAcidCompoundSet()));
        LinkedHashMap<String, ProteinSequence> b = fastaReader.process();
        for (Entry<String, ProteinSequence> entry : b.entrySet()) {
            System.out.println(entry.getValue().getOriginalHeader() + "=" + entry.getValue().getSequenceAsString());
        }
        */
    }



}
