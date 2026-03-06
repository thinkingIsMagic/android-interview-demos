# 面试题3：海量数据处理 - 答案参考

## 1. Hash分治法

```java
public void splitFileByHash(String inputFilePath, String outputDir, String filePrefix)
    throws IOException {

    BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
    BufferedWriter[] writers = new BufferedWriter[SPLIT_COUNT];

    for (int i = 0; i < SPLIT_COUNT; i++) {
        writers[i] = new BufferedWriter(
            new FileWriter(outputDir + "/" + filePrefix + "_" + i));
    }

    String line;
    while ((line = reader.readLine()) != null) {
        int index = Math.abs(line.hashCode()) % SPLIT_COUNT;
        writers[index].write(line);
        writers[index].newLine();
    }

    reader.close();
    for (BufferedWriter w : writers) w.close();
}

public void findCommonUrls(String dirA, String dirB, String outputFile) throws IOException {
    BufferedWriter result = new BufferedWriter(new FileWriter(outputFile));

    for (int i = 0; i < SPLIT_COUNT; i++) {
        Set<String> set = new HashSet<>();

        // 读取文件A
        BufferedReader readerA = new BufferedReader(
            new FileReader(dirA + "/a_" + i));
        String line;
        while ((line = readerA.readLine()) != null) {
            set.add(line);
        }
        readerA.close();

        // 对比文件B
        BufferedReader readerB = new BufferedReader(
            new FileReader(dirB + "/b_" + i));
        while ((line = readerB.readLine()) != null) {
            if (set.contains(line)) {
                result.write(line);
                result.newLine();
            }
        }
        readerB.close();
    }
    result.close();
}
```

## 2. 布隆过滤器

```java
public BloomFilter(int expectedNumber, double falsePositiveRate) {
    this.size = (int) (-expectedNumber * Math.log(falsePositiveRate) /
                       (Math.log(2) * Math.log(2)));
    this.hashCount = (int) (size / expectedNumber * Math.log(2));
    this.bitSet = new BitSet(size);
}

public void add(String value) {
    int[] positions = getHashPositions(value, hashCount, size);
    for (int pos : positions) {
        bitSet.set(pos);
    }
}

public boolean mightContain(String value) {
    int[] positions = getHashPositions(value, hashCount, size);
    for (int pos : positions) {
        if (!bitSet.get(pos)) return false;
    }
    return true;
}

private int[] getHashPositions(String value, int hashCount, int size) {
    int[] positions = new int[hashCount];
    int hash1 = value.hashCode();
    int hash2 = hash1 >>> 16;

    for (int i = 0; i < hashCount; i++) {
        positions[i] = Math.abs((hash1 + i * hash2) % size);
    }
    return positions;
}
```
