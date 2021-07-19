# Conda adapter benchmarks

To run benchmarks:
 1. Install snapshot locally of `conda-adapter`: `mvn install`
 2. Build `conda-bench` project: `mvn package -f ./benchmarks`
 3. Copy dependencies to `target/dependency/` directory: `mvn dependency:copy-dependencies`
 4. Create directory for tests and copy test resources to this directory
 5. Run benchmarks with `env BENCH_DIR=${test-dir} java -cp "benchmarks/target/benchmarks.jar" org.openjdk.jmh.Main ${bench-name}`, where `${test-dir}` is a directory with test data, and `${bench-name}` is a benchbmark name.

## Benchmarks

### CondaRepodataRemoveBench 

This benchmark removes packages records from `repodata.json` file by provided list of packages 
`sha256` checksum. `CondaRepodataRemoveBench` works with `com.artipie.conda.CondaRepodata.Remove` 
class and requires `repodata.json` file in the test directory. Example file can be found 
[here](https://artipie.s3.amazonaws.com/conda-test/conda-remove.tar.gz).