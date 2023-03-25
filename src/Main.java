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
	private static int monkeySymbolHeight = 8;
	private static int monkeySymbolWidth = 8;
	private static int fileDataMonkeyAddressHeightOffset = 50;
	private static int fileDataMonkeyAddressWidthOffset = 50;
	private static BufferedImage monkeySticker;
	private static boolean autoResize = true;

	public static void main(final String... args) throws IOException {
		if (args.length != 0)
			processFiles(args);
		repl();
	}

	private static void processFiles(final String... fileList) throws IOException {
		for (final String fileName : fileList)
			getFilesFromMonkeyCard(fileName);
		System.exit(0);
	}

	private static int makeMonkeyCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No filename given.");
		if (fileName == null)
			return tokens.length;
		final List<boolean[][]> symbolList;
		if (!autoResize) {
			symbolList = compress(fileData, monkeySymbolHeight, monkeySymbolWidth);
			autoResize = true;
		} else {
			symbolList = compress(fileData);
		}
		final boolean[][] fileDataMonkeyAddress = monkeySymbolListToMonkeyAddress(symbolList);

		final StringBuilder metadata = buildMetadata(fileDataMonkeyAddress.length, fileDataMonkeyAddress[0].length);
		final List<boolean[][]> metadataSymbolList = compress(metadata, 3, 3);
		final boolean[][] metadataMonkeyAddress = monkeySymbolListToMonkeyAddress(metadataSymbolList, 64);

		final int black = Color.BLACK.getRGB();
		final int white = Color.white.getRGB();
		final int metaRowOffset = (monkeySticker.getHeight() - metadataMonkeyAddress.length);
		final int metaColOffset = (monkeySticker.getWidth() - metadataMonkeyAddress[0].length);
		for (int row = 0; row < metadataMonkeyAddress.length; row++)
			for (int col = 0; col < metadataMonkeyAddress[0].length; col++)
				monkeySticker.setRGB(metaColOffset + col, metaRowOffset + row,
						metadataMonkeyAddress[row][col] ? black : white);

		final int rowOffset = monkeySticker.getHeight() - fileDataMonkeyAddress.length
				- fileDataMonkeyAddressHeightOffset;
		final int colOffset = monkeySticker.getWidth() - fileDataMonkeyAddress[0].length
				- fileDataMonkeyAddressWidthOffset;
		for (int row = 0; row < fileDataMonkeyAddress.length; row++)
			for (int col = 0; col < fileDataMonkeyAddress[0].length; col++)
				monkeySticker.setRGB(colOffset + col, rowOffset + row, fileDataMonkeyAddress[row][col] ? black : white);
		ImageIO.write(monkeySticker, "png", new File(fileName));
		fileData.setLength(0);
		return tokenIndex + 1;
	}

	private static void repl() throws IOException {
		greet();
		final Scanner scanner = new Scanner(System.in);
		do
			System.out.print("Simian > ");
		while (evaluate(scanner.nextLine().trim().split("\\s+")));
		bye();
	}

	private static void bye() {
		System.out.println("I hope you had fun with ICA MonkeyCard!");
		System.out.println("Thank you for playing Computational Monkeyposting with me!");
		System.exit(0);
	}

	private static boolean evaluate(final String[] tokens) throws IOException {
		for (int tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++)
			switch (tokens[tokenIndex].toUpperCase()) {
				case "ADDDIRECTORY" -> tokenIndex = addDirectoryToMonkeyCard(tokens, tokenIndex);
				case "ADDFILE" -> tokenIndex = addFileToMonkeyCard(tokens, tokenIndex);
				case "BYE" -> {
					return false;
				}
				case "CARD" -> tokenIndex = makeMonkeyCard(tokens, tokenIndex);
				case "OFFSET" -> tokenIndex = offset(tokens, tokenIndex);
				case "OUT" -> tokenIndex = output(tokens, tokenIndex);
				case "STICKER" -> tokenIndex = addStickerToMonkeyCard(tokens, tokenIndex);
				case "SYMBOL" -> tokenIndex = symbol(tokens, tokenIndex);
				case "UNCARD" -> tokenIndex = getFilesFromMonkeyCard(tokens, tokenIndex);
			}
		return true;
	}

	private static int addDirectoryToMonkeyCard(final String[] tokens, final int tokenIndex) throws IOException {
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
		fileDataMonkeyAddressHeightOffset = Integer.parseInt(height);
		fileDataMonkeyAddressWidthOffset = Integer.parseInt(width);
		return tokenIndex + 2;
	}

	private static int symbol(final String[] tokens, final int tokenIndex) {
		final String height = lookAhead(tokens, tokenIndex, "No height given.");
		final String width = lookAhead(tokens, tokenIndex + 1, "No width given.");
		if (height == null || width == null)
			return tokens.length;
		monkeySymbolHeight = Integer.parseInt(height);
		monkeySymbolWidth = Integer.parseInt(width);
		autoResize = false;
		return tokenIndex + 2;
	}

	private static int addFileToMonkeyCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No filename given.");
		if (fileName == null)
			return tokens.length;
		addFileToFileData(fileName);
		return tokenIndex + 1;
	}

	private static int addStickerToMonkeyCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No sticker file.");
		if (fileName == null)
			return tokens.length;
		monkeySticker = ImageIO.read(new File(fileName));
		return tokenIndex + 1;
	}

	private static void greet() {
		final Calendar calendar = Calendar.getInstance();
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour >= 4 && hour <= 11)
			System.out.println("Good Morning ICA Monkeys!");
		if (hour >= 12 && hour <= 16)
			System.out.println("Good Afternoon ICA Monkeys!");
		if (hour >= 17 && hour <= 20)
			System.out.println("Good Evening ICA Monkeys!");
		if (hour >= 21 || hour <= 3)
			System.out.println("Good Night ICA Monkeys!");
		System.out.println("Welcome to Kurumi MonkeyCard!\n");
	}

	public static String lookAhead(final String[] tokens, final int tokenIndex, final String errorMessage) {
		if (tokens.length > tokenIndex + 1)
			return tokens[tokenIndex + 1];
		System.out.println(errorMessage);
		return null;
	}

	private static int getFilesFromMonkeyCard(final String[] tokens, final int tokenIndex) throws IOException {
		final String fileName = lookAhead(tokens, tokenIndex, "No filename given.");
		if (fileName == null)
			return tokens.length;
		getFilesFromMonkeyCard(fileName);
		return tokenIndex + 1;
	}

	private static void getFilesFromMonkeyCard(final String fileName) throws IOException {
		final BufferedImage monkeyCard = ImageIO.read(new File(fileName));
		final int metadataHeight = 24;
		final int metadataWidth = 24;
		final int black = Color.BLACK.getRGB();
		final int metaDataColOffset = monkeyCard.getWidth() - metadataWidth;
		final int metaDataRowOffset = monkeyCard.getHeight() - metadataHeight;
		final boolean[][] metaDataMonkeyAddress = new boolean[metadataHeight][metadataWidth];
		for (int row = 0; row < metadataHeight; row++)
			for (int col = 0; col < metadataWidth; col++)
				metaDataMonkeyAddress[row][col] = monkeyCard.getRGB(metaDataColOffset + col,
						metaDataRowOffset + row) == black;
		final List<boolean[][]> metaDataMonkeySymbolList = monkeySpaceToMonkeySymbolList(metaDataMonkeyAddress, 3, 3);
		final StringBuilder metaData = decompress(metaDataMonkeySymbolList);
		final String[] metaTokens = metaData.toString().split(" ");
		monkeySymbolHeight = Integer.parseInt(metaTokens[0]);
		monkeySymbolWidth = Integer.parseInt(metaTokens[1]);
		final int fileMonkeyAddressHeight = Integer.parseInt(metaTokens[2]);
		final int fileMonkeyAddressWidth = Integer.parseInt(metaTokens[3]);
		final int zeroHeight = Integer.parseInt(metaTokens[4]);
		final int zeroWidth = Integer.parseInt(metaTokens[5]);

		final boolean[][] fileDataMonkeyAddress = new boolean[fileMonkeyAddressHeight][fileMonkeyAddressWidth];
		final int rowOffset = monkeyCard.getHeight() - fileMonkeyAddressHeight - zeroHeight;
		final int colOffset = monkeyCard.getWidth() - fileMonkeyAddressWidth - zeroWidth;
		for (int row = 0; row < fileDataMonkeyAddress.length; row++)
			for (int col = 0; col < fileDataMonkeyAddress[0].length; col++)
				fileDataMonkeyAddress[row][col] = monkeyCard.getRGB(colOffset + col, rowOffset + row) == black;
		final List<boolean[][]> fileDataMonkeySymbolList = monkeySpaceToMonkeySymbolList(fileDataMonkeyAddress,
				monkeySymbolHeight, monkeySymbolWidth);
		makeFiles(decompress(fileDataMonkeySymbolList));
	}

	private static StringBuilder buildMetadata(final int fileDataMonkeyAddressHeight,
			final int fileMonkeyAddressWidth) {
		return new StringBuilder()
				.append(monkeySymbolHeight)
				.append(" ")
				.append(monkeySymbolWidth)
				.append(" ")
				.append(fileDataMonkeyAddressHeight)
				.append(" ")
				.append(fileMonkeyAddressWidth)
				.append(" ")
				.append(fileDataMonkeyAddressHeightOffset)
				.append(" ")
				.append(fileDataMonkeyAddressWidthOffset);
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

	private static List<boolean[][]> monkeySpaceToMonkeySymbolList(final boolean[][] monkeySpace, final int height,
			final int width) {
		final int symbolHeight = monkeySpace.length / height;
		final int symbolWidth = monkeySpace[0].length / width;
		final List<boolean[][]> monkeySymbolList = new ArrayList<>();
		for (int row = symbolHeight - 1; row >= 0; row--)
			for (int col = symbolWidth - 1; col >= 0; col--) {
				final boolean[][] symbol = new boolean[height][width];
				for (int currentHeight = 0; currentHeight < height; currentHeight++)
					System.arraycopy(monkeySpace[(row * height) + currentHeight], (col * width), symbol[currentHeight],
							0,
							width);
				monkeySymbolList.add(symbol);
			}
		return monkeySymbolList;
	}

	private static boolean[][] monkeySymbolListToMonkeyAddress(final List<boolean[][]> monkeySymbolList,
			final int... capacity) {
		final int listSize = capacity.length > 0 ? capacity[0] : monkeySymbolList.size();
		final int cardWidth = (int) round(ceil(sqrt(listSize))) == 0 ? 1 : (int) round(ceil(sqrt(listSize)));
		final int cardCapacity = ((cardWidth * (cardWidth - 1) >= listSize) ? (cardWidth * (cardWidth - 1))
				: (cardWidth * cardWidth)) == 0 ? 1
						: ((cardWidth * (cardWidth - 1) >= listSize) ? (cardWidth * (cardWidth - 1))
								: (cardWidth * cardWidth));
		final int cardHeight = (int) ceil((double) cardCapacity / cardWidth);
		final int listItemHeight = monkeySymbolList.get(0).length;
		final int listItemWidth = monkeySymbolList.get(0)[0].length;
		final int monkeySpaceHeight = cardHeight * listItemHeight;
		final int monkeySpaceWidth = cardWidth * listItemWidth;
		final List<boolean[][]> paddedList = padMonkeySymbolList(monkeySymbolList, cardCapacity);
		final boolean[][] monkeyAddress = new boolean[monkeySpaceHeight][monkeySpaceWidth];
		for (int listIndex = 0; listIndex < paddedList.size(); listIndex++)
			for (int row = cardHeight - 1; row >= 0; row--)
				for (int col = cardWidth - 1; col >= 0; col--) {
					final boolean[][] listItem = paddedList.get(listIndex++);
					for (int currentListItemHeight = 0; currentListItemHeight < listItemHeight; currentListItemHeight++)
						System.arraycopy(listItem[currentListItemHeight], 0,
								monkeyAddress[currentListItemHeight + (row * listItemHeight)], col * listItemWidth,
								listItemWidth);
				}
		return monkeyAddress;
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

	// Monkey-LZW compress
	private static List<boolean[][]> compress(final StringBuilder text, final int... capacity) {
		final List<boolean[][]> monkeySymbolList = new ArrayList<>();
		if (capacity.length == 0)
			monkeySymbolHeight = monkeySymbolWidth = 8;
		else if (capacity.length == 1) {
			monkeySymbolHeight = (int) ceil(sqrt(capacity[0])) == 0 ? 1 : (int) ceil(sqrt(capacity[0]));
			monkeySymbolWidth = (int) ceil((double) capacity[0] / monkeySymbolHeight);
			System.out.println("Monkey Symbol capacity is " + capacity[0] + ".");
			System.out.println("Using (" + monkeySymbolHeight + ", " + monkeySymbolWidth + ")");
		} else {
			monkeySymbolHeight = capacity[0];
			monkeySymbolWidth = capacity[1];
		}

		final boolean[][] counter = new boolean[monkeySymbolHeight][monkeySymbolWidth];
		final Map<String, boolean[][]> dictionary = new LinkedHashMap<>();
		for (int character = 0; character < charsetLength; character++) {
			dictionary.put(String.valueOf((char) character), copyMonkeySpace(counter));
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
				monkeySymbolList.add(dictionary.get(w.toString()));
				dictionary.put(wc.toString(), copyMonkeySpace(counter));
				successor(counter);
				w.setLength(0);
				w.append(c);
			}
			wc.setLength(0);
		}
		if (dictionary.containsKey(w.toString()))
			monkeySymbolList.add(dictionary.get(w.toString()));
		if (capacity.length > 0)
			return monkeySymbolList;
		System.out.println("Auto resizing Monkey Symbols");
		return compress(text,
				((int) (ceil(Math.log((((((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1
						: (int) round(ceil(sqrt((dictionary.size() + 1)))))
						* (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1
								: (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1) >= (dictionary.size()
										+ 1)) ? (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1)) : (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * ((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))))) == 0 ? 1 : ((((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1) >= (dictionary.size() + 1)) ? (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) - 1)) : (((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1))))) * ((int) round(ceil(sqrt((dictionary.size() + 1)))) == 0 ? 1 : (int) round(ceil(sqrt((dictionary.size() + 1)))))))))
						/ Math.log(2)))));
	}

	// Monkey-LZW decompress
	private static StringBuilder decompress(final List<boolean[][]> compressed) {
		final Map<boolean[][], StringBuilder> dictionary = new TreeMap<>(Kurumi::compareMonkeySpace);
		final boolean[][] counter = new boolean[compressed.get(0).length][compressed.get(0)[0].length];
		for (int character = 0; character < charsetLength; character++) {
			dictionary.put(copyMonkeySpace(counter), new StringBuilder().append((char) character));
			successor(counter);
		}

		final StringBuilder w = new StringBuilder(dictionary.get(compressed.remove(0)));
		final StringBuilder result = new StringBuilder(w);
		final StringBuilder entry = new StringBuilder();
		for (final boolean[][] monkeySymbol : compressed) {
			if (dictionary.containsKey(monkeySymbol))
				entry.append(dictionary.get(monkeySymbol));
			else if (compareMonkeySpace(monkeySymbol, counter) == 0)
				entry.append(w).append(w.charAt(0));
			result.append(entry);
			if (entry.length() > 0)
				dictionary.put(copyMonkeySpace(counter), new StringBuilder(w.append(entry.charAt(0))));
			successor(counter);
			w.setLength(0);
			w.append(entry);
			entry.setLength(0);
		}
		return result;
	}

	private static List<boolean[][]> padMonkeySymbolList(final List<boolean[][]> list, final int capacity) {
		final boolean[][] eof = new boolean[list.get(0).length][list.get(0)[0].length];
		for (final boolean[] row : eof)
			Arrays.fill(row, true);
		final List<boolean[][]> paddedList = new ArrayList<>(list);
		while (paddedList.size() < capacity)
			paddedList.add(eof);
		return paddedList;
	}

	private static int compareMonkeySpace(final boolean[][] monkeySpace1, final boolean[][] monkeySpace2) {
		for (int row = 0; row < monkeySpace1.length; row++)
			for (int col = 0; col < monkeySpace1[0].length; col++)
				if (monkeySpace1[row][col] != monkeySpace2[row][col])
					return monkeySpace1[row][col] ? 1 : -1;
		return 0;
	}

	private static boolean[][] copyMonkeySpace(final boolean[][] monkeySpace) {
		final boolean[][] copy = new boolean[monkeySpace.length][monkeySpace[0].length];
		for (int row = 0; row < copy.length; row++)
			System.arraycopy(monkeySpace[row], 0, copy[row], 0, copy[row].length);
		return copy;
	}

	private static void successor(final boolean[][] monkeySpace) {
		for (int row = monkeySpace.length - 1; row >= 0; row--)
			for (int col = monkeySpace[row].length - 1; col >= 0; col--) {
				monkeySpace[row][col] = !monkeySpace[row][col];
				if (monkeySpace[row][col])
					return;
			}
	}
}
