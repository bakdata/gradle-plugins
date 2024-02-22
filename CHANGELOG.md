# Change Log
- no changes!

- no changes!

- no changes!

- no changes!

- no changes!

- no changes!


## [1.1.11](https://github.com/bakdata/gradle-plugins/tree/1.1.11) (2024-01-29)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.10...1.1.11)

**Merged pull requests:**

- Update Jacoco for Java 21 compatibility [\#32](https://github.com/bakdata/gradle-plugins/pull/32) ([@philipp94831](https://github.com/philipp94831))

## [1.1.10](https://github.com/bakdata/gradle-plugins/tree/1.1.10) (2024-01-29)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.9...1.1.10)

**Merged pull requests:**

- Update SonarQube Plugin [\#25](https://github.com/bakdata/gradle-plugins/pull/25) ([@philipp94831](https://github.com/philipp94831))

## [1.1.9](https://github.com/bakdata/gradle-plugins/tree/1.1.9) (2024-01-10)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.8...1.1.9)


## [1.1.8](https://github.com/bakdata/gradle-plugins/tree/1.1.8) (2024-01-10)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.7...1.1.8)

**Closed issues:**

- docs? [\#22](https://github.com/bakdata/gradle-plugins/issues/22)

**Merged pull requests:**

- Configure staging profile ID for Nexus release [\#31](https://github.com/bakdata/gradle-plugins/pull/31) ([@philipp94831](https://github.com/philipp94831))

## [1.1.7](https://github.com/bakdata/gradle-plugins/tree/1.1.7) (2021-09-02)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.6...1.1.7)

**Closed issues:**

- Update plugins [\#23](https://github.com/bakdata/gradle-plugins/issues/23)

**Merged pull requests:**

- Update plugins for Gradle 7 compatibility [\#24](https://github.com/bakdata/gradle-plugins/pull/24) ([@philipp94831](https://github.com/philipp94831))
- Improved deployment fault tolerance [\#15](https://github.com/bakdata/gradle-plugins/pull/15) ([@AHeise](https://github.com/AHeise))

## [1.1.6](https://github.com/bakdata/gradle-plugins/tree/1.1.6) (2021-02-08)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.5...1.1.6)

**Merged pull requests:**

- Use new variable group name [\#21](https://github.com/bakdata/gradle-plugins/pull/21) ([@philipp94831](https://github.com/philipp94831))

## [1.1.5](https://github.com/bakdata/gradle-plugins/tree/1.1.5) (2021-02-04)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.4...1.1.5)

**Closed issues:**

- Allow configuration of timeout when publishing to nexus [\#19](https://github.com/bakdata/gradle-plugins/issues/19)

**Merged pull requests:**

- Allow configurable timeouts and simplify local maven publishing [\#20](https://github.com/bakdata/gradle-plugins/pull/20) ([@torbsto](https://github.com/torbsto))

## [1.1.4](https://github.com/bakdata/gradle-plugins/tree/1.1.4) (2019-02-28)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.2...1.1.4)

**Fixed bugs:**

- Cannot upload release to sonatype in parallel [\#16](https://github.com/bakdata/gradle-plugins/issues/16)

**Merged pull requests:**

- Fixed empty plugins \(no compilation of Kotlin code\) [\#18](https://github.com/bakdata/gradle-plugins/pull/18) ([@AHeise](https://github.com/AHeise))
- Bumped nexus\-publish\-plugin to 0.2.0 to solve parallel release issue \#16 [\#17](https://github.com/bakdata/gradle-plugins/pull/17) ([@AHeise](https://github.com/AHeise))

## [1.1.2](https://github.com/bakdata/gradle-plugins/tree/1.1.2) (2019-02-21)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.1...1.1.2)

**Fixed bugs:**

- Coverage always at 0% [\#13](https://github.com/bakdata/gradle-plugins/issues/13)

**Merged pull requests:**

- Bugfix incorrect repo name in multi module [\#12](https://github.com/bakdata/gradle-plugins/pull/12) ([@AHeise](https://github.com/AHeise))
- Correctly applying jacoco plugin to all java projects [\#14](https://github.com/bakdata/gradle-plugins/pull/14) ([@AHeise](https://github.com/AHeise))

## [1.1.1](https://github.com/bakdata/gradle-plugins/tree/1.1.1) (2019-02-21)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.1.0...1.1.1)

**Merged pull requests:**

- Fixing the release changelog post\-deploy script [\#11](https://github.com/bakdata/gradle-plugins/pull/11) ([@AHeise](https://github.com/AHeise))
- Sonar plugin uses the same java project detection as in sonatype [\#10](https://github.com/bakdata/gradle-plugins/pull/10) ([@AHeise](https://github.com/AHeise))

## [1.1.0](https://github.com/bakdata/gradle-plugins/tree/1.1.0) (2019-02-20)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.0.2...1.1.0)

**Implemented enhancements:**

- More intelligent publishing detection [\#7](https://github.com/bakdata/gradle-plugins/issues/7)

**Merged pull requests:**

- Fixed settings for changelog creation [\#9](https://github.com/bakdata/gradle-plugins/pull/9) ([@AHeise](https://github.com/AHeise))
- To detect projects with publications, look for non\-empty source sets [\#8](https://github.com/bakdata/gradle-plugins/pull/8) ([@AHeise](https://github.com/AHeise))
- Improved error handling in .travis.yml [\#6](https://github.com/bakdata/gradle-plugins/pull/6) ([@AHeise](https://github.com/AHeise))

## [1.0.2](https://github.com/bakdata/gradle-plugins/tree/1.0.2) (2019-02-20)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.0.1...1.0.2)

**Fixed bugs:**

- DisallowLocalRelease not working [\#1](https://github.com/bakdata/gradle-plugins/issues/1)

**Implemented enhancements:**

- Add changelog generation [\#3](https://github.com/bakdata/gradle-plugins/issues/3)
- Add github release [\#2](https://github.com/bakdata/gradle-plugins/issues/2)

**Merged pull requests:**

- Adding github release and changelog generation [\#5](https://github.com/bakdata/gradle-plugins/pull/5) ([@AHeise](https://github.com/AHeise))
- Deferred evaluation of SonatypeSettings\#disallowLocalRelease after ta… [\#4](https://github.com/bakdata/gradle-plugins/pull/4) ([@AHeise](https://github.com/AHeise))

## [1.0.1](https://github.com/bakdata/gradle-plugins/tree/1.0.1) (2019-02-05)
[Full Changelog](https://github.com/bakdata/gradle-plugins/compare/1.0.0...1.0.1)


## [1.0.0](https://github.com/bakdata/gradle-plugins/tree/1.0.0) (2019-02-01)

