#ifndef LIB_TEST
#define LIB_TEST

#include <Balau/Testing/TestRunner.hpp>

struct LibTest : public Balau::Testing::TestGroup<LibTest> {
	LibTest() {
		registerTest(&LibTest::test, "test");
	}

	void test();
};

#endif // LIB_TEST
