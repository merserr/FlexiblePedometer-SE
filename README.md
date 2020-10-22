﻿**FlexiblePedometer - это шагомер под Android**

# Краткое описание
Взято у https://github.com/AlexDrob/FlexiblePedometer

Для графиков используется https://github.com/PhilJay/MPAndroidChart


Немного переделано под себя.
Добалены каллории.
Шагомер для своей работы использует данные акселерометра. В приложении во главу угла было поставлено пройденное расстояние, а не количество сделанных шагов.
Добавлена адаптация вычисления расстояния от скорости ходьбы (длина шага зависит от скорости ходьбы).

Приложение совместимо (не проверял) с Android 4.4 (API 19) и выше.
Настраивал на Xiaomi Redmi Note 9, Android 10, MIUI 12.0.1.
Для того, чтобы сервис не останавливался системой когда ей вздумается, необходимо установить "замочек" на приложении:

![Image](/screenshots/11.jpg)

# Описание основного функционала
На главном экране приложения можно посмотреть пройденное за сегодня расстояние, время, скорость, количество шагов и потраченные каллории. Кнопка запускает/останавливает подсчет шагов. На графике отображается расстояние, которое было пройдено за каждый час. Таким образом, можно визуально оценить насколько, например, большее или меньшее расстояние было пройдено вечером нежели утром. Кнопка Измерение вызывает режим Измерение, который позволяет засечь время и пройденное расстояние. Удобно для определения расстояния между пунктами А и Б.

![Image](/screenshots/2.jpg)

На notification для foreground сервиса показывается пройденное расстояние за сегодня, а также находится кнопка включения/выключения шагомера. Таким образом, одним движением можно посмотреть пройденное расстояние, двумя - запустить/остановить шагомер.

![Image](/screenshots/3.jpg)

# Дополнительный функционал и вкладки
![Image](/screenshots/4.jpg)

## Домой 
Возврат на главный экран шагомера

## Измерение
Позволяет засечь время и пройденное расстояние. Удобно для определения расстояния между пунктами А и Б. 

![Image](/screenshots/5.jpg)

## История
Позволяет просмотреть историю за день, неделю или месяц. 

![Image](/screenshots/6.jpg)

## Удалить историю
Позволяет удалить историю, требует при этом дополнительного подтверждения.

![Image](/screenshots/7.jpg)

## Настройки
Позволяет настроить порог чувствительности, ширину шага и ожидаемое расстояние, которое проходится за день. Первые два параметра используются для правильного подсчета шагов и расстояния. Последний параметр используется для цветового отображения пройденного расстояния на главном экране.

![Image](/screenshots/8.jpg)
![Image](/screenshots/9.jpg)

## Лог Файл
Показывает содержимое лог файла, в который записывается время начала и конца измерения расстояния, вычисленная длина каждого шага

![Image](/screenshots/10.jpg)

## Выход
Выход из приложения с остановкой foreground сервиса
