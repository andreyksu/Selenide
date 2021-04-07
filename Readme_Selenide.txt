TODO: Minor Детальнее изучить класс WebDriverManager (но это сторонний класс/либа по отношению к selenide)
TODO: Детальнее изучить в классе WebDriverThreadLocalContainer > List<WebDriverEventListener> listeners и EventFiringWebDriver цели и задачи.
        WebDriverEventListener - это класс самого Selenium. А вот как и где он используется в Selenide.
TODO: Заняться изучением SelenidePageFactory, Modals, DownloadFileWithHttpRequest.
        Проанализировать в чем различие в применении FileDownloadFilter и DownloadFileWithHttpRequest. Т.е. само различие понятно, DownloadFileWithHttpRequest скачивает по непосредственной ссылке.
        А вот FileDownloadFilter скачивает из ответа по Content-Disposition. Вообще нужно более детальнее изучить и может описать (ибо не понятно, скачиваение происходит для ответа POST)?

Selenide

1. open("url")								-	В тесте вызывается метод.
                                                - Важно, если нужно использовать внешний proxy то следует перед началом создания драйвера т.е. перед open() сделать WebDriverRunner.setProxy(Proxy proxy). При создании драйвера, данный внешний прокси будет обернут в SelenideServerProxy.
	1.1. open("url")						-	Вызывает static метод класса Selenide
	1.2. open ("url", ...)					-	Вызывается метод у объекта Navigator
	1.2.1. navigateTo ("url" ...)			-	Вызывается private метод у объекта Navigator. 
													- В этом метод проверяется:
													    - Нужен ли Proxy т.е. по сути SelenideProxyServer (определяется через параметры "selenide.fileDownload" либо -Dselenide.fileDownload=PROXY Т.е. если они указаны, то прокси будет включен автоматически).
													    - Задан ли абсолютный или относительный путь (если относительный то добавляется вначало переменная "selenide.baseUrl"). А обсолютность проверяется по наличию спереди протокола http: или https: или file:.
													    - Нужна ли Авторизация (информация берется из propery, что задается при старте приложения).
													    - Если авторизация нужна то в методе beforeNavigateTo(...) определяется как будет добавляться информация об авторизации
                                                            - Если будет использован Proxy то аутентификация будет проходит с использованием AuthenticationFilter (т.е. будет добавляться информация в этом фильтре)
                                                            - Если proxy нет, то информация по аутентификации будет прходить сразу добавляя в URL
	1.3. getAndCheckWebDriver()				-	Вызывается static метод класса WebDriverRunner
	1.4. getAddCheckWebDriver()				-	Вызывается метод у объекта WebDriverThreadLocalContainer
													- Здесь для каждого потока создается свой инстенс WebDriver (хранится в потокобезопасном Map<Thread, WebDriver>)
													    - Для каждого потока создается свой инстанс WebDriver, при этом в качестве ключа для получения инстенса используется ID потока.
													- По аналогии для каждого экземпляра webDriver создается своя proxy. Т.е. proxy тоже является привяазнной к потоку.
													    - Нужно разделять две прокси:
													        - внешний прокси - к примеру для доступа к ресурсу (доступ к интернету).
													        - прокси, что создается Selenide для перехвата трафика (для удобства логгирования) см. SelenideProxyServer. При этом SelenideProxy прокси принимает на вход внешний прокси для работы с ним (т.е. оборачивает).
													            - Так же к прокси добавляются свои фильтры. К примеру FileDownloadFilter - который позволяет парсить трафик и скачивать файлы. Или AuthenticationFilter - который добавляет аутентификационную информацию к запуросу.
	1.4.1. createDriver						-	Вызывается метод protected у объекта WebDriverThreadLocalContainer
													- В этом методе происходит обращение к Factory
	1.5. createWebDriver					-	Вызывается метод  у объекта WebDriverFactory
													- Здесь находятся коллекция фабрик для каждого конкретного браузера.
													- Предварительно обращается к объекту WebDriverBinaryManager - который смотрит property, узнает какой брауезер задан, потом смотрит, задан ли путь до driver в пропертях для этого браузера, если нет то обращается к WebDriverManager скачивает и устанавливает соответствующий webDriver.
													    - Тут же проверяется какая платформа итд (но это уже отдельный код, что не относится к Selenide).
													- Бежим по коллекции этих фабрик, каждая фабрика проверят переменную "selenide.browser" и если нашли совпадение, то создаем инстанс и возвращаем WebDriver иначе будет создан WebDriver по умолчанию.

										На этом все!

2. $(By)									-	В тесте вызывается метод
	2.1. $(By)								-	Вызывается static метод в Selenide
	2.2. getElement(By)						-	Вызывается static метод в Selenide
	2.3. wrap(null, By, 0)					-	Вызывается static метод в ElementFinder
	2.4. wrap(Class, null, By, 0)			-	Вызывается static метод в ElementFinder
													- Здесь создается и возвращается proxy с типом SelenideElement

	2.5. На этом оканчивается работа т.к. вернули Proxy с типом SelenideElement.											
											Примечание:
														- В будущем все вызовы методов SelenideElement будут идти через метод invoke класса SelenideElementProxy.
                                                            - SelenideElementProxy внутри себя содержит экземпляр ElementFinder - который, хранит информацию о селкторе и родителе при его наличии.
                                                                - По сути proxy это есть SelenideElement - и у него нет таких методов как "дай родителя" итд. По этому в Хэндлере SelenideElementProxy содержится поле которое хранит в себе всю эту информацию. Этим поле является ElementFinder.
                                                                    - По этой причине ElementFinder называется по коду webElementSource.
                                                                    - Если родителя нет, то по сути это терминальный(родительский элемент) и у него для поиска используется webDriver
														- SelenideElementProxy будет делегировать вызовы методов в Commands.
															- Commands в свою очередь хранит обрабочики вызовов в Map<String, Command> - находим по имени метод и вызываем команду. При этом команда это объект.
															- В Commands передается кроме самого Proxy еще и ElementFinder который по сути выполняет роль locator


	Дальше, если на этом SelenideElement вызывается метод:
	2.1. Будет перехват вызова метода в invoke.
	2.2. Будет выбрана команда из Commands - на базе информации какой метод у proxy был вызван. И этой команде передадутся параметры.

		2.2.1. Допустим если этот метод будет find(String cssSelector) - то этот вызов будет перехвачен в методе invoke и передан вызов в Commands
		2.2.2. В Commands будет создана команда/объект Find и у него вызван метод execute(proxy, locatorWebElementSource, args)
		2.2.3. Так как предполагается искать внутри proxy объекта SelenideElement - то ищем внутри его locatorWebElementSource, а текущий proxy заворачиваем как родителя для нового proxy.
		    Т.е. команда find - поиск не производит и не производит действий, она просто оборачивает и возвращает новый соответствующий proxy (и у этого proxy предыдущий прокси является родителем).

	2.3. Вот когда вызывается команда click() - происходит по сути уже сам поиск а лишь потом клик.
	    2.3.1. Бежим и разворачиваем ранее завернутые объекты.
	        2.3.1.1. В классе Click вызывается в методе execute(...) у locator (т.е. у завернутой переменной locatorWebElementSource в proxy) метод findAndAssertElementIsVisible() а он checkCondition(...) тот в свою очередь вызывает getWebElement();
	        2.3.1.2. Если он был завернут в proxy то при получении в классе ElementFinder getSearchContext(), который вызовет метод toWebElement() обратимся к proxy для которого перехватится вызов и создастся объект ToWebElement который снова вызовит getWebElement() у родителя и так пока не закончатся родители и не дойдем до webDriver.


    2.4. Все методы вроде should итд в конечном счете обращается к locator.checkCondition(....). А здесь все зависит от того какое статическое поле класса Condition было передано.
            Каждое из этих статических полей являются наследниками Condition а он в свою очередь от

-------------
	2.2.1. click()									- У прокси вызываем этот метод.
	2.2.2. invoke()									- Перехватывается вызов идем в Commands и там создаем объект Click() у него вызываем метод execute()
	2.2.3. locator.findAndAssertElementIsVisible 		- Внутре объекта Click вызываем у WebElementSource/ElementFinder метод findAndAssertElementIsVisible
	2.2.4. checkCondition								- Внутренний вызов метода в WebElementSource
	2.2.5. getWebElement								- Внутренний вызов метода в ElementFinder
	2.2.6. findElement(getSearchContext(), creteria)	- Вызов в WebElementSelector - основной класс где производится сам поиск.

	getSearchContext() -	если parent == null то webDriver()
							иначе parent.toWebElement()				- где parent по сути proxy. Это вызов тоже проксируется и создается объект ToWebElement - а тот снова getWebElement() п. 2.5 для родителя и так пока не будет в методе getSearchContext() условия [parent == null то webDriver()]

3. $$(By)
                        Здесь уже работа ведется через ElementsCollection и BySelectorCollection
Изучить отдельно/дополнительно.
------------------------------------------------------------------------------



------------------------------------------------------------------------------
Navigator

