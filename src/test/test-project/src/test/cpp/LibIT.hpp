#ifndef LIB_IT
#define LIB_IT

#include <Balau/Testing/TestRunner.hpp>

struct LibIT : public Balau::Testing::TestGroup<LibIT> {
	LibIT() {
		registerTest(&LibIT::test, "test");
	}

	void test();
};

#endif // LIB_IT
