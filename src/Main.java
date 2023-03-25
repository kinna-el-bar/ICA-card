import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static java.lang.Math.*;

public class Main {
	private final static int charsetLength = 256;
	private static final StringBuilder fileData = new StringBuilder();
	private static String outDir = "card/";
	private static int maidSymbolHeight = 8;
	private static int maidSymbolWidth = 8;
	private static int fileDataMaidAddressHeightOffset = 50;
	private static int fileDataMaidAddressWidthOffset = 50;
	private static BufferedImage maidSticker;
	private static boolean autoResize = true;

	public static void main(final String... args) throws IOException {
		if (args.length != 0)
			processFiles(args);
		repl();
	}

	private static void processFiles(final String... fileList) throws IOException {
		for (final String fileName : fileList)
			getFilesFromMaidCard(fileName);
		System.exit(0);
	}

	private static int makeMaidCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No filename given.");
		if (fileName == null)
			return tokens.length;
		final List<boolean[][]> symbolList;
		if (!autoResize) {
			symbolList = compress(fileData, maidSymbolHeight, maidSymbolWidth);
			autoResize = true;
		} else {
			symbolList = compress(fileData);
		}
		final boolean[][] fileDataMaidAddress = maidSymbolListToMaidAddress(symbolList);

		final StringBuilder metadata = buildMetadata(fileDataMaidAddress.length, fileDataMaidAddress[0].length);
		final List<boolean[][]> metadataSymbolList = compress(metadata, 3, 3);
		final boolean[][] metadataMaidAddress = maidSymbolListToMaidAddress(metadataSymbolList, 64);

		final int black = Color.BLACK.getRGB();
		final int white = Color.white.getRGB();
		final int metaRowOffset = (maidSticker.getHeight() - metadataMaidAddress.length);
		final int metaColOffset = (maidSticker.getWidth() - metadataMaidAddress[0].length);
		for (int row = 0; row < metadataMaidAddress.length; row++)
			for (int col = 0; col < metadataMaidAddress[0].length; col++)
				maidSticker.setRGB(metaColOffset + col, metaRowOffset + row,
						metadataMaidAddress[row][col] ? black : white);

		final int rowOffset = maidSticker.getHeight() - fileDataMaidAddress.length - fileDataMaidAddressHeightOffset;
		final int colOffset = maidSticker.getWidth() - fileDataMaidAddress[0].length - fileDataMaidAddressWidthOffset;
		for (int row = 0; row < fileDataMaidAddress.length; row++)
			for (int col = 0; col < fileDataMaidAddress[0].length; col++)
				maidSticker.setRGB(colOffset + col, rowOffset + row, fileDataMaidAddress[row][col] ? black : white);
		ImageIO.write(maidSticker, "png", new File(fileName));
		fileData.setLength(0);
		return tokenIndex + 1;
	}

	private static void repl() throws IOException {
		greet();
		final Scanner scanner = new Scanner(System.in);
		do
			System.out.print("Kurumi> ");
		while (evaluate(scanner.nextLine().trim().split("\\s+")));
		bye();
	}

	private static void bye() {
		System.out.println("I hope you had fun with Kurumi MaidCard!");
		System.out.println("Thank you for playing Computational Maidposting with me!");
		System.exit(0);
	}

	private static boolean evaluate(final String[] tokens) throws IOException {
		for (int tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++)
			switch (tokens[tokenIndex].toUpperCase()) {
				case "ADDDIRECTORY" -> tokenIndex = addDirectoryToMaidCard(tokens, tokenIndex);
				case "ADDFILE" -> tokenIndex = addFileToMaidCard(tokens, tokenIndex);
				case "BYE" -> {
					return false;
				}
				case "CARD" -> tokenIndex = makeMaidCard(tokens, tokenIndex);
				case "OFFSET" -> tokenIndex = offset(tokens, tokenIndex);
				case "OUT" -> tokenIndex = output(tokens, tokenIndex);
				case "STICKER" -> tokenIndex = addStickerToMaidCard(tokens, tokenIndex);
				case "SYMBOL" -> tokenIndex = symbol(tokens, tokenIndex);
				case "UNCARD" -> tokenIndex = getFilesFromMaidCard(tokens, tokenIndex);
			}
		return true;
	}

	private static int addDirectoryToMaidCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String directory = lookAhead(tokens, tokenIndex, "No directory specified.");
		if (directory == null)
			return tokens.length;
		final Path dir = Paths.get(directory);
		Files.walk(dir).forEach(path -> {
			try {
				addFileToFileData(path.toFile());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		});
		return tokenIndex + 1;
	}

	private static void addFileToFileData(final File file) throws IOException {
		if (file.isDirectory())
			return;
		addFileToFileData(file.getPath().replaceAll("\\\\", "/"));
	}

	private static int output(final String[] tokens, final int tokenIndex) {
		final String output = lookAhead(tokens, tokenIndex, "No output directory given.");
		if (output == null)
			return tokens.length;
		outDir = output + "/";
		return tokenIndex + 1;
	}

	private static int offset(final String[] tokens, final int tokenIndex) {
		final String height = lookAhead(tokens, tokenIndex, "No height given.");
		final String width = lookAhead(tokens, tokenIndex + 1, "No width given.");
		if (height == null || width == null)
			return tokens.length;
		fileDataMaidAddressHeightOffset = Integer.parseInt(height);
		fileDataMaidAddressWidthOffset = Integer.parseInt(width);
		return tokenIndex + 2;
	}

	private static int symbol(final String[] tokens, final int tokenIndex) {
		final String height = lookAhead(tokens, tokenIndex, "No height given.");
		final String width = lookAhead(tokens, tokenIndex + 1, "No width given.");
		if (height == null || width == null)
			return tokens.length;
		maidSymbolHeight = Integer.parseInt(height);
		maidSymbolWidth = Integer.parseInt(width);
		autoResize = false;
		return tokenIndex + 2;
	}

	private static int addFileToMaidCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No filename given.");
		if (fileName == null)
			return tokens.length;
		addFileToFileData(fileName);
		return tokenIndex + 1;
	}

	private static int addStickerToMaidCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No sticker file.");
		if (fileName == null)
			return tokens.length;
		maidSticker = ImageIO.read(new File(fileName));
		return tokenIndex + 1;
	}

	private static void greet() {
		final Calendar calendar = Calendar.getInstance();
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour >= 4 && hour <= 11)
			System.out.println("Good Morning Dra/g/on Maids!");
		if (hour >= 12 && hour <= 16)
			System.out.println("Good Afternoon Dra/g/on Maids!");
		if (hour >= 17 && hour <= 20)
			System.out.println("Good Evening Dra/g/on Maids!");
		if (hour >= 21 || hour <= 3)
			System.out.println("Good Night Dra/g/on Maids!");
		System.out.println("Welcome to Kurumi MaidCard!\n");
	}

	public static String lookAhead(final String[] tokens, final int tokenIndex, final String errorMessage) {
		if (tokens.length > tokenIndex + 1)
			return tokens[tokenIndex + 1];
		System.out.println(errorMessage);
		return null;
	}

	private static int getFilesFromMaidCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No filename given.");
		if (fileName == null)
			return tokens.length;
		getFilesFromMaidCard(fileName);
		return tokenIndex + 1;
	}

	private static void getFilesFromMaidCard(final String fileName) throws IOException {
		final BufferedImage maidCard = ImageIO.read(new File(fileName));
		final int metadataHeight = 24;
		final int metadataWidth = 24;
		final int black = Color.BLACK.getRGB();
		final int metaDataColOffset = maidCard.getWidth() - metadataWidth;
		final int metaDataRowOffset = maidCard.getHeight() - metadataHeight;
		final boolean[][] metaDataMaidAddress = new boolean[metadataHeight][metadataWidth];
		for (int row = 0; row < metadataHeight; row++)
			for (int col = 0; col < metadataWidth; col++)
				metaDataMaidAddress[row][col] = maidCard.getRGB(metaDataColOffset + col,
						metaDataRowOffset + row) == black;
		final List<boolean[][]> metaDataMaidSymbolList = maidSpaceToMaidSymbolList(metaDataMaidAddress, 3, 3);
		final StringBuilder metaData = decompress(metaDataMaidSymbolList);
		final String[] metaTokens = metaData.toString().split(" ");
		maidSymbolHeight = Integer.parseInt(metaTokens[0]);
		maidSymbolWidth = Integer.parseInt(metaTokens[1]);
		final int fileMaidAddressHeight = Integer.parseInt(metaTokens[2]);
		final int fileMaidAddressWidth = Integer.parseInt(metaTokens[3]);
		final int zeroHeight = Integer.parseInt(metaTokens[4]);
		final int zeroWidth = Integer.parseInt(metaTokens[5]);

		final boolean[][] fileDataMaidAddress = new boolean[fileMaidAddressHeight][fileMaidAddressWidth];
		final int rowOffset = maidCard.getHeight() - fileMaidAddressHeight - zeroHeight;
		final int colOffset = maidCard.getWidth() - fileMaidAddressWidth - zeroWidth;
		for (int row = 0; row < fileDataMaidAddress.length; row++)
			for (int col = 0; col < fileDataMaidAddress[0].length; col++)
				fileDataMaidAddress[row][col] = maidCard.getRGB(colOffset + col, rowOffset + row) == black;
		final List<boolean[][]> fileDataMaidSymbolList = maidSpaceToMaidSymbolList(fileDataMaidAddress,
				maidSymbolHeight, maidSymbolWidth);
		makeFiles(decompress(fileDataMaidSymbolList));
	}

	private static StringBuilder buildMetadata(final int fileDataMaidAddressHeight, final int fileMaidAddressWidth) {
		return new StringBuilder()
				.append(maidSymbolHeight)
				.append(" ")
				.append(maidSymbolWidth)
				.append(" ")
				.append(fileDataMaidAddressHeight)
				.append(" ")
				.append(fileMaidAddressWidth)
				.append(" ")
				.append(fileDataMaidAddressHeightOffset)
				.append(" ")
				.append(fileDataMaidAddressWidthOffset);
	}

	private static void makeFiles(final StringBuilder decompressed) throws IOException {
		final String[] lines = decompressed.toString().split("\n");
		String fileName = "noname.txt";
		final StringBuilder fileContents = new StringBuilder();
		for (int index = 0; index < lines.length; index++)
			switch (lines[index]) {
				case "[FN]" -> fileName = lines[++index];
				case "[EOF]" -> {
					fileContents.deleteCharAt(fileContents.length() - 1);
					writeFile(fileName, fileContents.toString());
					fileContents.setLength(0);
				}
				default -> fileContents.append(lines[index]).append("\n");
			}
	}

	private static void writeFile(final String fileName, final String fileContents) throws IOException {
		final String directoryName = fileName.substring(0, fileName.lastIndexOf("/"));
		Files.createDirectories(Paths.get(directoryName));
		final File file = new File(fileName);
		final FileWriter fileWriter = new FileWriter(file, StandardCharsets.ISO_8859_1);
		System.out.println("Writing: " + fileName);
		fileWriter.write(fileContents);
		fileWriter.flush();
		fileWriter.close();
	}

	private static List<boolean[][]> maidSpaceToMaidSymbolList(final boolean[][] maidSpace, final int height,
			final int width) {
		final int symbolHeight = maidSpace.length / height;
		final int symbolWidth = maidSpace[0].length / width;
		final List<boolean[][]> maidSymbolList = new ArrayList<>();
		for (int row = symbolHeight - 1; row >= 0; row--)
			for (int col = symbolWidth - 1; col >= 0; col--) {
				final boolean[][] symbol = new boolean[height][width];
				for (int currentHeight = 0; currentHeight < height; currentHeight++)
					System.arraycopy(maidSpace[(row * height) + currentHeight], (col * width), symbol[currentHeight], 0,
							width);
				maidSymbolList.add(symbol);
			}
		return maidSymbolList;
	}

	private static boolean[][] maidSymbolListToMaidAddress(final List<boolean[][]> maidSymbolList,
			final int... capacity) {
		final int listSize = capacity.length > 0 ? capacity[0] : maidSymbolList.size();
		final int cardWidth = (int) round(ceil(sqrt(listSize))) == 0 ? 1 : (int) round(ceil(sqrt(listSize)));
		final int cardCapacity = ((cardWidth * (cardWidth - 1) >= listSize) ? (cardWidth * (cardWidth - 1))
				: (cardWidth * cardWidth)) == 0 ? 1
						: ((cardWidth * (cardWidth - 1) >= listSize) ? (cardWidth * (cardWidth - 1))
								: (cardWidth * cardWidth));
		final int cardHeight = (int) ceil((double) cardCapacity / cardWidth);
		final int listItemHeight = maidSymbolList.get(0).length;
		final int listItemWidth = maidSymbolList.get(0)[0].length;
		final int maidSpaceHeight = cardHeight * listItemHeight;
		final int maidSpaceWidth = cardWidth * listItemWidth;
		final List<boolean[][]> paddedList = padMaidSymbolList(maidSymbolList, cardCapacity);
		final boolean[][] maidAddress = new boolean[maidSpaceHeight][maidSpaceWidth];
		for (int listIndex = 0; listIndex < paddedList.size(); listIndex++)
			for (int row = cardHeight - 1; row >= 0; row--)
				for (int col = cardWidth - 1; col >= 0; col--) {
					final boolean[][] listItem = paddedList.get(listIndex++);
					for (int currentListItemHeight = 0; currentListItemHeight < listItemHeight; currentListItemHeight++)
						System.arraycopy(listItem[currentListItemHeight], 0,
								maidAddress[currentListItemHeight + (row * listItemHeight)], col * listItemWidth,
								listItemWidth);
				}
		return maidAddress;
	}

	private static void addFileToFileData(final String fileName) throws IOException {
		final Scanner fileReader = new Scanner(new File(fileName), StandardCharsets.ISO_8859_1);
		if (fileData.length() != 0)
			fileData.append("\n");
		fileData.append("[FN]\n").append(outDir).append(fileName).append("\n");
		while (fileReader.hasNextLine())
			fileData.append(fileReader.nextLine()).append("\n");
		fileReader.close();
		fileData.append("[EOF]");
	}

	// Maid-LZW compress
	private static List<boolean[][]> compress(final StringBuilder text, final int... capacity) {
		final List<boolean[][]> maidSymbolList = new ArrayList<>();
		if (capacity.length == 0)
			maidSymbolHeight = maidSymbolWidth = 8;
		else if (capacity.length == 1) {
			maidSymbolHeight = (int) ceil(sqrt(capacity[0])) == 0 ? 1 : (int) ceil(sqrt(capacity[0]));
			maidSymbolWidth = (int) ceil((double) capacity[0] / maidSymbolHeight);
			System.out.println("Maid Symbol capacity is " + capacity[0] + ".");
			System.out.println("Using (" + maidSymbolHeight + ", " + maidSymbolWidth + ")");
		} else {
			maidSymbolHeight = capacity[0];
			maidSymbolWidth = capacity[1];
		}

		final boolean[][] counter = new boolean[maidSymbolHeight][maidSymbolWidth];
		final Map<String, boolean[][]> dictionary = new LinkedHashMap<>();
		for (int character = 0; character < charsetLength; character++) {
			dictionary.put(String.valueOf((char) character), copyMaidSpace(counter));
			successor(counter);
		}
		final StringBuilder w = new StringBuilder();
		final StringBuilder wc = new StringBuilder();
		for (final char c : text.toString().toCharArray()) {
			wc.append(w).append(c);
			if (dictionary.containsKey(wc.toString())) {
				w.setLength(0);
				w.append(wc);
			} else if (dictionary.containsKey(w.toString())) {
				maidSymbolList.add(dictionary.get(w.toString()));
				dictionary.put(wc.toString(), copyMaidSpace(counter));
				successor(counter);
				w.setLength(0);
				w.append(c);
			}
			wc.setLength(0);
		}
		if (dictionary.containsKey(w.toString()))
			maidSymbolList.add(dictionary.get(w.toString()));
		if (capacity.length > 0)
			return maidSymbolList;
		System.out.println("Auto resizing Maid Symbols");
		return compress(text,
				((int) (ceil(Math.log((((((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1
						: (int) round(ceil(sqrt((dictionary.size() + 1)))))
						* (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1
								: (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1) >= (dictionary.size()
										+ 1)) ? (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1)) : (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * ((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))))) == 0 ? 1 : ((((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1) >= (dictionary.size() + 1)) ? (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1)) : (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * ((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1)))))))))
						/ Math.log(2)))));
	}

	// Maid-LZW decompress
	private static StringBuilder decompress(final List<boolean[][]> compressed) {
		final Map<boolean[][], StringBuilder> dictionary = new TreeMap<>(Kurumi::compareMaidSpace);
		final boolean[][] counter = new boolean[compressed.get(0).length][compressed.get(0)[0].length];
		for (int character = 0; character < charsetLength; character++) {
			dictionary.put(copyMaidSpace(counter), new StringBuilder().append((char) character));
			successor(counter);
		}

		final StringBuilder w = new StringBuilder(dictionary.get(compressed.remove(0)));
		final StringBuilder result = new StringBuilder(w);
		final StringBuilder entry = new StringBuilder();
		for (final boolean[][] maidSymbol : compressed) {
			if (dictionary.containsKey(maidSymbol))
				entry.append(dictionary.get(maidSymbol));
			else if (compareMaidSpace(maidSymbol, counter) == 0)
				entry.append(w).append(w.charAt(0));
			result.append(entry);
			if (entry.length() > 0)
				dictionary.put(copyMaidSpace(counter), new StringBuilder(w.append(entry.charAt(0))));
			successor(counter);
			w.setLength(0);
			w.append(entry);
			entry.setLength(0);
		}
		return result;
	}

	private static List<boolean[][]> padMaidSymbolList(final List<boolean[][]> list, final int capacity) {
		final boolean[][] eof = new boolean[list.get(0).length][list.get(0)[0].length];
		for (final boolean[] row : eof)
			Arrays.fill(row, true);
		final List<boolean[][]> paddedList = new ArrayList<>(list);
		while (paddedList.size() < capacity)
			paddedList.add(eof);
		return paddedList;
	}

	private static int compareMaidSpace(final boolean[][] maidSpace1, final boolean[][] maidSpace2) {
		for (int row = 0; row < maidSpace1.length; row++)
			for (int col = 0; col < maidSpace1[0].length; col++)
				if (maidSpace1[row][col] != maidSpace2[row][col])
					return maidSpace1[row][col] ? 1 : -1;
		return 0;
	}

	private static boolean[][] copyMaidSpace(final boolean[][] maidSpace) {
		final boolean[][] copy = new boolean[maidSpace.length][maidSpace[0].length];
		for (int row = 0; row < copy.length; row++)
			System.arraycopy(maidSpace[row], 0, copy[row], 0, copy[row].length);
		return copy;
	}

	private static void successor(final boolean[][] maidSpace) {
		for (int row = maidSpace.length - 1; row >= 0; row--)
			for (int col = maidSpace[row].length - 1; col >= 0; col--) {
				maidSpace[row][col] = !maidSpace[row][col];
				if (maidSpace[row][col])
					return;
			}
	}
}
